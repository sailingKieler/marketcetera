package org.marketcetera.ui.view;

import org.marketcetera.ui.events.TileWindowsEvent;
import org.springframework.stereotype.Component;

import javafx.scene.image.Image;


/* $License$ */

/**
 * Provides a menu item to cascade open windows.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@Component
public class TileWindowsMenuItem
        extends AbstractMenuItem
{
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.MenuContent#getMenuCaption()
     */
    @Override
    public String getMenuCaption()
    {
        return "Tile Windows";
    }
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.MenuContent#getWeight()
     */
    @Override
    public int getWeight()
    {
        return 200;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.MenuContent#getCategory()
     */
    @Override
    public MenuContent getCategory()
    {
        return WindowContentCategory.instance;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.MenuContent#getMenuIcon()
     */
    @Override
    public Image getMenuIcon()
    {
        return getIcon("images/Tile_Windows.png");
    }
    /* (non-Javadoc)
     * @see org.marketcetera.web.view.MenuContent#getCommand()
     */
    @Override
    public Runnable getCommand()
    {
        return new Runnable() {
            @Override
            public void run()
            {
                webMessageService.post(new TileWindowsEvent());
            }
        };
    }
}
