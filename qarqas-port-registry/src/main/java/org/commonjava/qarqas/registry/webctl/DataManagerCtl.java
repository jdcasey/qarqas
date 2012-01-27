package org.commonjava.qarqas.registry.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.qarqas.registry.data.PortDataException;
import org.commonjava.qarqas.registry.data.PortDataManager;

@WebListener
public class DataManagerCtl
    implements ServletContextListener
{

    @Inject
    private PortDataManager data;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        try
        {
            data.initialize();
        }
        catch ( final PortDataException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
        data.destroy();
    }

}
