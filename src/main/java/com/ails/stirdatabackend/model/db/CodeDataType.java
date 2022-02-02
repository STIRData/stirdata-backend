package com.ails.stirdatabackend.model.db;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import com.ails.stirdatabackend.model.Code;

public class CodeDataType extends AbstractSingleColumnStandardBasicType<Code> {

	public static final CodeDataType INSTANCE = new CodeDataType();

	public CodeDataType() {
		super(VarcharTypeDescriptor.INSTANCE, CodeDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "Code";
	}
	
}