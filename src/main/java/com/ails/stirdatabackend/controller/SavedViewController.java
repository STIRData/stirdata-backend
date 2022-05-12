package com.ails.stirdatabackend.controller;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import com.ails.stirdatabackend.model.SavedView;
import com.ails.stirdatabackend.payload.Message;
import com.ails.stirdatabackend.payload.SavedViewCreationDTO;
import com.ails.stirdatabackend.payload.SavedViewReturnAllDTO;
import com.ails.stirdatabackend.security.UserPrincipal;
import com.ails.stirdatabackend.service.SavedViewService;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/view")
public class SavedViewController {
    
    private final SavedViewService savedViewService;

    @Autowired
    public SavedViewController(SavedViewService savedViewService) {
        this.savedViewService = savedViewService;
    }

    @GetMapping()
    public ResponseEntity<?> getUserViews(@AuthenticationPrincipal UserPrincipal user) {
        try {
            List<SavedViewReturnAllDTO> viewList = savedViewService.getUserSavedViews(user.getId());
            return ResponseEntity.status(HttpStatus.OK).body(viewList);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Message(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getView(@AuthenticationPrincipal UserPrincipal user, @PathVariable ObjectId id) {
        try {
            Optional<SavedView> viewOpt = savedViewService.getSavedViewById(user.getId(), id);
            if (!viewOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.status(HttpStatus.OK).body(viewOpt.get());
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Message(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteView(@AuthenticationPrincipal UserPrincipal user, @PathVariable ObjectId id) {
        try {
            boolean deleted = savedViewService.deleteSavedView(user.getId(), id);
            if (deleted) {
                return ResponseEntity.status(HttpStatus.OK).body(null);
            }
            else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Message(e.getMessage()));
        }
    }

    @PostMapping()
    public ResponseEntity<?> createView(@AuthenticationPrincipal UserPrincipal user, @RequestBody @Valid SavedViewCreationDTO request ) {
        try {
            SavedView createdView = savedViewService.createView(user.getId(), request);
            return ResponseEntity.status(HttpStatus.OK).body(createdView);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Message(e.getMessage()));
        }
    }

}
