package org.marketcetera.photon.views.fixmessagedetail.dialogs.executionreport.data;

import java.math.BigDecimal;

import org.marketcetera.photon.Messages;

import quickfix.Message;
import quickfix.field.CumQty;

public class CumQtyField extends ExecutionReportField 
{	
	@Override
	public String getFieldName() 
	{
		return Messages.EXECUTION_REPORT_FIELD_CUM_QTY.getText();
	}

	@Override
	public String[] getValues() 
	{
		return NULL_VALUE;
	}

	@Override
	public void insertField(Message message) 
	{
		message.setField(new CumQty(new BigDecimal(fValue)));	
	}

	@Override
	public boolean validateValue() {
		if(!super.validateValue())
		{
			return false;
		}
		try
		{
			new BigDecimal(fValue);
		}
		catch(NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}
}
