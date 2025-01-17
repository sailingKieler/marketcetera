package org.marketcetera.web.trade.fixmessagedetails.view;

import org.marketcetera.web.trade.openorders.view.OpenOrderView;
import org.marketcetera.web.view.AbstractContentViewFactory;
import org.marketcetera.web.view.ContentView;

import com.vaadin.spring.annotation.SpringComponent;

/* $License$ */

/**
 * Creates {@link OpenOrderView} content objects.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@SpringComponent
public class FixMessageDetailsViewFactory
        extends AbstractContentViewFactory
{
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.AbstractContentViewFactory#getViewType()
     */
    @Override
    protected Class<? extends ContentView> getViewType()
    {
        return FixMessageDetailsView.class;
    }
}
