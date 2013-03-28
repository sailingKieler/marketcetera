package org.marketcetera.photon.views.fixmessagedetail.dialogs.executionreport.data;

import java.util.ArrayList;
import java.util.List;

import org.marketcetera.trade.SecurityType;

import quickfix.Message;

public class SecurityTypeField extends ExecutionReportField {

	@Override
	public String getFieldName() {
		return "Security Type";
	}

	@Override
	public String[] getValues() 
	{
		List<String> securityTypeValues = new ArrayList<String>();
		for(SecurityType orderType: SecurityType.values())
		{
			securityTypeValues.add(orderType.name());
		}
	
		return (String[]) securityTypeValues.toArray(new String[securityTypeValues.size()]);
	}

	@Override
	public void insertField(Message message) {
		message.setField(new quickfix.field.SecurityType(SecurityType.valueOf(fValue).getFIXValue()));
		
	}

}
