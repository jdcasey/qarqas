package org.commonjava.qarqas.registry.data;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.commonjava.qarqas.registry.model.PortConfiguration;

public abstract class AbstractPortDataManager
    implements PortDataManager
{

    private boolean initialized;

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

        new Timer( true ).schedule( new ExpireTask( this ), 1000, 1000 );

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
