package org.jacorb.orb.dns;

import java.net.InetAddress;
/**
 * DNSLookup.java
 *
 *
 * Created: Thu Apr  5 10:45:02 2001
 *
 * @author Nicolas Noffke
 * @version $Id: DNSLookup.java,v 1.1 2003-04-03 16:52:59 wbeebee Exp $
 */

public class DNSLookup  
{
    private static DNSLookupDelegate delegate = null;

    static
    {
        createDelegate();
    }

    private static void createDelegate()
    {
        try
        {
            Class c = 
                Class.forName( "org.jacorb.orb.dns.DNSLookupDelegateImpl" );
            
            delegate = (DNSLookupDelegate) c.newInstance();
        }
        catch( Exception e )
        {
            //ignore
        }
    }

    public static String inverseLookup( String ip )
    {
        if( delegate != null )
        {
            return delegate.inverseLookup( ip );
        }
        else
        {
            return null;
        }
    }

    public static String inverseLookup( InetAddress addr )
    {
        if( delegate != null )
        {
            return delegate.inverseLookup( addr );
        }
        else
        {
            return null;
        }
    }
            
} // DNSLookup
