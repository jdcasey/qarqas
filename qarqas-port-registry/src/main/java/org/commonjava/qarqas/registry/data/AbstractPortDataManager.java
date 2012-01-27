package org.commonjava.qarqas.registry.data;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.commonjava.qarqas.registry.model.PortConfiguration;
import org.commonjava.util.logging.Logger;

public abstract class AbstractPortDataManager
    implements PortDataManager
{

    protected final Logger logger = new Logger( getClass() );

    private boolean initialized;

    private Timer timer;

    @Override
    @PreDestroy
    public void destroy()
    {
        logger.info( "Stopping expiration timer..." );
        timer.cancel();
        timer = null;
    }

    @PostConstruct
    @Override
    public synchronized void initialize()
        throws PortDataException
    {
        if ( initialized )
        {
            return;
        }

        final Set<PortConfiguration> reservations = PortDataInitializer.createReservationDefinitions();
        for ( final PortConfiguration reservation : reservations )
        {
            defineConfiguration( reservation );
        }

        timer = new Timer( true );
        timer.schedule( new ExpireTask( this ), 1000, 1000 );

        initialized = true;
    }

    protected abstract void defineConfiguration( PortConfiguration reservation )
        throws PortDataException;

    private final class ExpireTask
        extends TimerTask
    {
        private final AbstractPortDataManager mgr;

        ExpireTask( final AbstractPortDataManager mgr )
        {
            this.mgr = mgr;
        }

        @Override
        public void run()
        {
            mgr.clearExpiredReservations();
        }
    }
}
