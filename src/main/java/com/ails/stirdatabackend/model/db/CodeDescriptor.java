package com.ails.stirdatabackend.model.db;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

import com.ails.stirdatabackend.model.Code;

public class CodeDescriptor extends AbstractTypeDescriptor<Code> {

    public static final CodeDescriptor INSTANCE = new CodeDescriptor();

    public CodeDescriptor() {
        super(Code.class, ImmutableMutabilityPlan.INSTANCE);
    }
	
    @Override
    public <X> X unwrap(Code value, Class<X> type, WrapperOptions options) {

        if (value == null)
            return null;

        if (String.class.isAssignableFrom(type)) {
            return  (X)(value.getNamespace() + ":" + value.getCode());
        }

        throw unknownUnwrap(type);
    }
    
    @Override
    public <X> Code wrap(X value, WrapperOptions options) {
        if (value == null)
            return null;

        if (String.class.isInstance(value)) {
            return fromString((String)value);
        }

        throw unknownWrap(value.getClass());
    }

	@Override
	public Code fromString(String string) {
//		String[] r = string.split(":");
//		return new Code(r[0], r[1]);	
		return new Code(string);
	}
}