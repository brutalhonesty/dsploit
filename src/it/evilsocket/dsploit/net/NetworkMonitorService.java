/*
 * This file is part of the dSploit.
 *
 * Copyleft of Simone Margaritelli aka evilsocket <evilsocket@gmail.com>
 *
 * dSploit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dSploit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dSploit.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.evilsocket.dsploit.net;

import it.evilsocket.dsploit.R;
import it.evilsocket.dsploit.net.Endpoint;
import it.evilsocket.dsploit.net.Network;
import it.evilsocket.dsploit.net.Target;
import it.evilsocket.dsploit.tools.NMap.FindAliveEndpointsOutputReceiver;
import it.evilsocket.dsploit.core.System;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class NetworkMonitorService extends Service
{
	public static final String TAG				 = "NETWORKMONITORSERVICE";
	
	public static final String NEW_ENDPOINT		 = "NetworkMonitorService.action.NEW_ENDPOINT";
	public static final String ENDPOINT_ADDRESS  = "NetworkMonitorService.data.ENDPOINT_ADDRESS";
	public static final String ENDPOINT_HARDWARE = "NetworkMonitorService.data.ENDPOINT_HARDWARE";
	
	public static boolean      RUNNING			 = false;
	
	private Network			  				 mNetwork 		 = null;
	private FindAliveEndpointsOutputReceiver mReceiver  	 = null;
	private boolean							 mRunning  		 = false;
	private int								 mNotificationId = 0;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		
	}
	
	private void sendNotification( String message ) {
		if( System.getSettings().getBoolean( "PREF_NETWORK_NOTIFICATIONS", true ) )
		{
			NotificationManager manager 	 = ( NotificationManager )getSystemService(Context.NOTIFICATION_SERVICE);
			Notification 		notification = new Notification( R.drawable.dsploit_icon_48 , message, java.lang.System.currentTimeMillis() );
			Context 			context 	 = getApplicationContext();
			PendingIntent	    pending		 = PendingIntent.getActivity( context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT );
	
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			
			notification.setLatestEventInfo( context, "Network Monitor", message, pending );
			
			manager.cancel( mNotificationId );
			manager.notify( ++mNotificationId, notification );
		}
	}
	
	private void sendNewEndpointNotification( Endpoint endpoint ) {
		sendNotification( "New endpoint found on network : " + endpoint.toString() );
		
		Intent intent = new Intent( NEW_ENDPOINT );
		
		intent.putExtra( ENDPOINT_ADDRESS,  endpoint.toString() );
		intent.putExtra( ENDPOINT_HARDWARE, endpoint.getHardwareAsString() );
        sendBroadcast(intent);    	
	}

	@Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
    	super.onStartCommand(intent, flags, startId);
    	
    	Log.d( TAG, "Starting ..." );
    	
    	if( !mRunning )
    	{    		
    		mRunning = true;
    		RUNNING  = true;
    		
    		sendNotification( "Network monitor started ..." );
    		
    		try 
    		{
    			mNetwork = System.getNetwork();
    		} 
    		catch( Exception e )
    		{
    			
    		}
    		
    		mReceiver = new FindAliveEndpointsOutputReceiver(){			    			
    			@Override
    			public void onEndpointFound( Endpoint endpoint ) {
    				Target target = new Target( endpoint );
    				
    				if( System.hasTarget( target ) == false )
    				{    					
    					sendNewEndpointNotification( endpoint );	            
    				}
    			}
    			
    			@Override
    			public void onEnd( int code )
    			{
    				if( mRunning )
    					System.getNMap().findAliveEndpoints( mNetwork, mReceiver ).start();
    			}
    		};
    		
    		System.getNMap().findAliveEndpoints( mNetwork, mReceiver ).start();		
    	}
    	
    	return START_NOT_STICKY;
    }

	@Override
	public void onDestroy() {
		RUNNING  = false;
		mRunning = false;
		
		Log.d( TAG, "Stopping ..." );
		
		NotificationManager manager = ( NotificationManager )getSystemService(Context.NOTIFICATION_SERVICE);
		
		for( int i = mNotificationId; i >= 0; i-- )
			manager.cancel( i );
		
		System.getNMap().kill();
	}
}
