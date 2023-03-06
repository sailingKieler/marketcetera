package org.marketcetera.web.session.view;

import java.util.Collections;
import java.util.Set;

import org.marketcetera.admin.AdminPermissions;
import org.marketcetera.web.events.NewWindowEvent;
import org.marketcetera.web.font.MarketceteraFont;
import org.marketcetera.web.service.WebMessageService;
import org.marketcetera.web.view.AbstractContentViewFactory;
import org.marketcetera.web.view.ContentView;
import org.marketcetera.web.view.ContentViewFactory;
import org.marketcetera.web.view.MenuContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;

import com.google.common.collect.Sets;
import com.vaadin.server.Resource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

/* $License$ */

/**
 * Creates {@link SessionView} content objects.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@SpringComponent
public class SessionViewFactory
        extends AbstractContentViewFactory
        implements MenuContent
{
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.MenuContent#getMenuCaption()
     */
    @Override
    public String getMenuCaption()
    {
        return "FIX Sessions";
    }
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.MenuContent#getWeight()
     */
    @Override
    public int getWeight()
    {
        return 300;
    }
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.MenuContent#getCategory()
     */
    @Override
    public MenuContent getCategory()
    {
        return SessionContentCategory.instance;
    }
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.MenuContent#getMenuIcon()
     */
    @Override
    public Resource getMenuIcon()
    {
        return MarketceteraFont.FIX_Sessions;
    }
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.MenuContent#getCommand()
     */
    @Override
    public Command getCommand()
    {
        return new MenuBar.Command() {
            @Override
            public void menuSelected(MenuItem inSelectedItem)
            {
                webMessageService.post(new NewWindowEvent() {
                    @Override
                    public String getWindowTitle()
                    {
                        return getMenuCaption();
                    }
                    @Override
                    public Class<? extends ContentViewFactory> getViewFactoryType()
                    {
                        return SessionViewFactory.class;
                    }}
                );
            }
            private static final long serialVersionUID = 49365592058433460L;
        };
    }
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.MenuContent#getAllPermissions()
     */
    @Override
    public Set<GrantedAuthority> getAllPermissions()
    {
        return requiredPermissions;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.AbstractContentViewFactory#getViewType()
     */
    @Override
    protected Class<? extends ContentView> getViewType()
    {
        return SessionView.class;
    }
    /**
     * permission(s) required to execute open order view
     */
    private static final Set<GrantedAuthority> requiredPermissions = Collections.unmodifiableSet(Sets.newHashSet(AdminPermissions.ViewSessionAction));
    /**
     * provides access to web message services
     */
    @Autowired
    private WebMessageService webMessageService;
}