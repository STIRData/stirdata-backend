package com.ails.stirdatabackend.security;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.GoogleAccountUserInfoDTO;
import com.ails.stirdatabackend.payload.ResultDTO;
import com.ails.stirdatabackend.service.UserService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OAuthService {

    @Autowired
    private UserService userService;
    private RestTemplate restTemplate;

    @Value("${app.oauth.google.url.userinfo}")
    private String googleUrl;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public OAuthService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ResultDTO<String> googleOauthVerify(String token) {
        String url =  googleUrl + "?id_token=" + token;
        GoogleAccountUserInfoDTO jsonResponse = restTemplate.getForObject(url, GoogleAccountUserInfoDTO.class);

        Optional<User> u = userService.checkAndCreateNewUserGoogle(jsonResponse);
        if (!u.isPresent()) {
            return ResultDTO.fail("Google Login failed. Account with email already exists");
        }

        String jwt = tokenProvider.generateToken(u.get().getId().toString());

        return ResultDTO.ok(jwt);
    }

    public ResultDTO<String> solidOauthVerify(String token) throws JsonParseException, IOException, Exception {

        // Parse solid jwt, get the payload, decode it, get the WebID
        String[] tokenParts = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();

        String jwtPayload = new String(decoder.decode(tokenParts[1]));

        ObjectMapper om = new ObjectMapper();
        JsonNode jwtPayloadJson = om.readTree(jwtPayload);

        String target = jwtPayloadJson.get("sub").textValue();

        // Hit solid endpoint to get jsonld, load it in-memory in JENA, 
        // perform sparql queries to get name, email, organization

        Model model = RDFDataMgr.loadModel(target, Lang.JSONLD);
        String sparql = "";

        sparql =
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
            "PREFIX vcard: <http://www.w3.org/2006/vcard/ns#> "+

            "SELECT ?name ?mail ?organization WHERE {" +
            "<"+target+">" + " foaf:name " + "?name ."+
            "OPTIONAL { "+
                "<"+target+">" + " vcard:organization-name " + "?organization . }" +
            "OPTIONAL { "+    
                "<"+target+">" + " vcard:hasEmail " + "?mailId ."+
                "?mailId vcard:value ?mail . }" +

            "}";
        
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, model)) {
            ResultSet rs = qe.execSelect();
            String jwt = "";
            if (rs.hasNext()) {
                QuerySolution qs = rs.next();
                Optional<String> name, organization;
                String email = "";
                name = qs.get("name") != null ? Optional.of(qs.get("name").asLiteral().toString()) : Optional.empty(); 
                organization = qs.get("organization") != null ? Optional.of(qs.get("organization").asLiteral().toString()) : Optional.empty(); 

                if (qs.get("mail") != null) {
                    String mail = qs.get("mail").asResource().getURI();
                    if (mail.startsWith("mailto:")) {
                        mail = mail.substring(7);
                    }
                    email = mail;
                }
                else {
                    throw new Exception("Solid login error");
                }

                Optional<User> u = userService.checkAndCreateNewUserSolid(email, name, organization);
                if (!u.isPresent()) {
                    return ResultDTO.fail("Solid login failed. Email in use")
                }

                jwt = tokenProvider.generateToken(u.get().getId().toString());
            }
            return ResultDTO.ok(jwt);
            
        }
    }

}
