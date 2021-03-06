package org.commonjava.qarqas.registry.data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Singleton;

import org.commonjava.qarqas.registry.model.PortConfiguration;

@Singleton
public class MemoryPortDataManager
    extends AbstractPortDataManager
{

    private final Map<Integer, PortConfiguration> all = new TreeMap<Integer, PortConfiguration>();

    private final HashMap<String, PortConfiguration> reserved = new HashMap<String, PortConfiguration>();

    private final TreeSet<PortConfiguration> unreserved = new TreeSet<PortConfiguration>();

    private final TreeSet<PortConfiguration> banned = new TreeSet<PortConfiguration>();

    private final Map<PortConfiguration, Date> expirations = new HashMap<PortConfiguration, Date>();

    private static final long LEASE_PERIOD = 5 * 60 * 1000; // 5 minutes.

    public MemoryPortDataManager()
        throws PortDataException
    {
        initialize();
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.arqas.registry.data.PortDataManager#reserve()
     */
    @Override
    public PortConfiguration reserve( final String clientKey, final Long expiration )
        throws PortDataException
    {
        if ( unreserved.isEmpty() )
        {
            return null;
        }

        PortConfiguration reservation = null;
        for ( final PortConfiguration config : unreserved )
        {
            if ( validate( config ) )
            {
                reservation = config;
                break;
            }

        }

        if ( reservation != null )
        {
            logger.info( "RESERVE FOR CLIENT: %s\n%s", clientKey, reservation );
            unreserved.remove( reservation );
            reserved.put( clientKey, reservation );

            renew( reservation, expiration );
        }

        return reservation;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.commonjava.arqas.registry.data.PortDataManager#unreserve(org.commonjava.arqas.registry.model.PortReservation)
     */
    @Override
    public synchronized void release( final PortConfiguration reservation, final String clientKey )
    {
        if ( reservation.equals( reserved.get( clientKey ) ) )
        {
            logger.info( "RELEASE FROM CLIENT: %s\n%s", clientKey, reservation );
            reserved.remove( clientKey );
            unreserved.add( reservation );
            clearExpiration( reservation );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.arqas.registry.data.PortDataManager#unreserve(java.lang.Integer)
     */
    @Override
    public synchronized void release( final Integer reservationKey, final String clientKey )
    {
        final PortConfiguration reservation = all.get( reservationKey );
        release( reservation, clientKey );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.arqas.registry.data.PortDataManager#ban(org.commonjava.arqas.registry.model.PortReservation)
     */
    @Override
    public synchronized void ban( final PortConfiguration reservation )
    {
        logger.info( "BAN: %s", reservation );
        remove( reservation );
        unreserved.remove( reservation );
        banned.add( reservation );
        clearExpiration( reservation );
    }

    private void remove( final PortConfiguration reservation )
    {
        for ( final Map.Entry<String, PortConfiguration> entry : new HashMap<String, PortConfiguration>( reserved ).entrySet() )
        {
            if ( entry.getValue()
                      .equals( reservation ) )
            {
                reserved.remove( entry.getKey() );
                return;
            }
        }
    }

    private void clearExpiration( final PortConfiguration reservation )
    {
        expirations.remove( reservation );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.arqas.registry.data.PortDataManager#ban(java.lang.Integer)
     */
    @Override
    public synchronized void ban( final Integer reservationKey )
    {
        final PortConfiguration reservation = all.get( reservationKey );
        ban( reservation );
    }

    @Override
    protected void defineConfiguration( final PortConfiguration reservation )
        throws PortDataException
    {
        if ( validate( reservation ) )
        {
            logger.info( "DEFINE: %s", reservation );
            all.put( reservation.getKey(), reservation );
            unreserved.add( reservation );
        }
    }

    private boolean validate( final PortConfiguration reservation )
    {
        final Integer port = reservation.getPort( "http" );
        boolean valid = reservation.isSane();
        if ( valid )
        {
            InetAddress localhost = null;
            Socket sock = null;
            try
            {
                localhost = InetAddress.getByAddress( new byte[] { 0x7f, 0x0, 0x0, 0x1 } );
                sock = new Socket( localhost, port );
                logger.info( "Port configuration in use at: %s:%s (from configuration:: %s)", localhost, port,
                             reservation );
                valid = false;
            }
            catch ( final UnknownHostException e )
            {
                valid = true;
            }
            catch ( final IOException e )
            {
                valid = true;
            }
            finally
            {
                if ( sock != null )
                {
                    try
                    {
                        sock.close();
                    }
                    catch ( final IOException e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        return valid;
    }

    @Override
    public void clearExpiredReservations()
    {
        final Date current = new Date();
        for ( final Map.Entry<PortConfiguration, Date> entry : new HashMap<PortConfiguration, Date>( expirations ).entrySet() )
        {
            final PortConfiguration reservation = entry.getKey();
            final Date d = entry.getValue();
            if ( current.after( d ) )
            {
                logger.info( "EXPIRE: %s", reservation );
                remove( reservation );
                unreserved.add( reservation );
                expirations.remove( reservation );
            }
        }
    }

    @Override
    public PortConfiguration getReservation( final String clientKey )
        throws PortDataException
    {
        return reserved.get( clientKey );
    }

    @Override
    public void renew( final PortConfiguration reservation, final Long expiration )
        throws PortDataException
    {
        long expires = System.currentTimeMillis();
        if ( expiration == null || expiration > LEASE_PERIOD * 2 )
        {
            expires += LEASE_PERIOD;
        }
        else
        {
            expires += expiration;
        }

        expirations.put( reservation, new Date( expires ) );
    }

}
