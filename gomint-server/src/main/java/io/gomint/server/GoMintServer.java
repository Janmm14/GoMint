/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server;

import io.gomint.GoMint;
import io.gomint.plugin.PluginManager;
import io.gomint.server.assets.AssetsLibrary;
import io.gomint.server.config.ServerConfig;
import io.gomint.server.crafting.Recipe;
import io.gomint.server.crafting.RecipeManager;
import io.gomint.server.network.NetworkManager;
import io.gomint.server.plugin.SimplePluginManager;
import io.gomint.server.scheduler.SyncScheduledTask;
import io.gomint.server.scheduler.SyncTaskManager;
import io.gomint.server.world.WorldAdapter;
import io.gomint.server.world.WorldManager;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author BlackyPaw
 * @author geNAZt
 * @version 1.1
 */
public class GoMintServer implements GoMint {

	private final Logger logger = LoggerFactory.getLogger( GoMintServer.class );

	// Global tick lock
	private ReentrantLock tickLock = new ReentrantLock( true );

    // Configuration
	@Getter
	private ServerConfig        serverConfig;

	// Networking
	private NetworkManager      networkManager;

	// World Management
	private WorldManager 		worldManager;

	// Game Information
	private RecipeManager       recipeManager;

	// Plugin Management
	private PluginManager       pluginManager;

	// Task Scheduling
	@Getter
	private SyncTaskManager     syncTaskManager;
	private AtomicBoolean       running = new AtomicBoolean( true );
	@Getter
	private ExecutorService     executorService;
	@Getter
	private ThreadFactory		threadFactory;

	/**
	 * Starts the GoMint server
	 * @param args which should have been given over from the static Bootstrap
	 */
	public GoMintServer( String[] args ) {
		Thread.currentThread().setName( "GoMint Main Thread" );

		// ------------------------------------ //
		// Executor Initialization
		// ------------------------------------ //
		this.threadFactory = new ThreadFactory() {
			private AtomicLong counter = new AtomicLong( 0 );

			@Override
			public Thread newThread( Runnable r ) {
				Thread thread = Executors.defaultThreadFactory().newThread( r );
				thread.setName( "GoMint Thread #" + counter.getAndIncrement() );
				return thread;
			}
		};

		this.executorService = new ThreadPoolExecutor( 0, 512, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), this.threadFactory );

		// ------------------------------------ //
		// Configuration Initialization
		// ------------------------------------ //
		this.loadConfig();

		// Calculate the nanoseconds we need for the tick loop
		long skipNanos = TimeUnit.SECONDS.toNanos( 1 ) / this.getServerConfig().getTargetTPS();
		logger.debug( "Setting skipNanos to: " + skipNanos );

		// ------------------------------------ //
		// Scheduler + PluginManager Initialization
		// ------------------------------------ //
		this.syncTaskManager = new SyncTaskManager( this, skipNanos );
		this.networkManager = new NetworkManager( this );
		this.pluginManager = new SimplePluginManager( this );

		// ------------------------------------ //
		// Networking Initialization
		// ------------------------------------ //
		if ( !this.initNetworking() ) return;

		// ------------------------------------ //
		// Pre World Initialization
		// ------------------------------------ //
		// Load assets from file:
		this.logger.info( "Loading assets library..." );
		AssetsLibrary assetsLibrary = new AssetsLibrary( LoggerFactory.getLogger( AssetsLibrary.class ) );
		try {
			assetsLibrary.load( this.getClass().getResourceAsStream( "/assets.dat" ) );
		} catch ( IOException e ) {
			this.logger.error( "Failed to load assets library", e );
			return;
		}

		this.logger.info( "Initializing recipes..." );
		this.recipeManager = new RecipeManager( this );

		// Add all recipes from asset library:
		for ( Recipe recipe : assetsLibrary.getRecipes() ) {
			this.recipeManager.registerRecipe( recipe );
		}

		// ------------------------------------ //
		// World Initialization
		// ------------------------------------ //
		this.worldManager = new WorldManager( this );
		try {
			this.worldManager.loadWorld( this.serverConfig.getWorld() );
		} catch ( Exception e ) {
			this.logger.error( "Failed to load default world", e );
		}

		// ------------------------------------ //
		// Main Loop
		// ------------------------------------ //
		// Tick loop
        float lastTickTime = Float.MIN_NORMAL;
        Condition tickCondition = tickLock.newCondition();

		while ( this.running.get() ) {
			this.tickLock.lock();
			try {
				long start = System.nanoTime();

                // Tick networking at every tick
                this.networkManager.tick();

				// Tick all major subsystems:
                long currentMillis = System.currentTimeMillis();
                this.syncTaskManager.update( currentMillis, lastTickTime );
				this.worldManager.update( currentMillis, lastTickTime );

				long diff = System.nanoTime() - start;
                lastTickTime = diff / 1000000;

				if ( diff < skipNanos ) {
                    tickCondition.await( skipNanos - diff, TimeUnit.NANOSECONDS );
				}
			} catch ( InterruptedException e ) {
				// Ignored ._.
			} finally {
				this.tickLock.unlock();
			}
		}
	}

	private boolean initNetworking() {
		try {
			this.networkManager.initialize( this.serverConfig.getMaxPlayers(), this.serverConfig.getListener().getIp(), this.serverConfig.getListener().getPort() );

			if ( this.serverConfig.isEnablePacketDumping() ) {
				File dumpDirectory = new File( this.serverConfig.getDumpDirectory() );
				if ( !dumpDirectory.exists() ) {
					if ( !dumpDirectory.mkdirs() ) {
						this.logger.error( "Failed to create dump directory; please double-check your filesystem permissions" );
						return false;
					}
				} else if ( !dumpDirectory.isDirectory() ) {
					this.logger.error( "Dump directory path does not point to a valid directory" );
					return false;
				}

				this.networkManager.setDumpingEnabled( true );
				this.networkManager.setDumpDirectory( dumpDirectory );
			}
		} catch ( SocketException e ) {
			this.logger.error( "Failed to initialize networking", e );
			return false;
		}

		return true;
	}

	public WorldAdapter getDefaultWorld() {
		return this.worldManager.getWorld( this.serverConfig.getWorld() );
	}

	public RecipeManager getRecipeManager() {
		return this.recipeManager;
	}

	private void loadConfig() {
		this.serverConfig = new ServerConfig();

		try {
			this.serverConfig.initialize( new File( "server.cfg" ) );
		} catch ( IOException e ) {
			logger.error( "server.cfg is corrupted: ", e );
			System.exit( -1 );
		}

		try ( FileWriter fileWriter = new FileWriter( new File( "server.cfg" ) ) ) {
			this.serverConfig.write( fileWriter );
		} catch ( IOException e ) {
			logger.warn( "Could not save server.cfg: ", e );
		}
	}

	@Override
	public String getMotd() {
		return this.networkManager.getMotd();
	}

	@Override
	public void setMotd( String motd ) {
		this.networkManager.setMotd( motd );
	}

	/**
	 * Nice shutdown pls
	 */
	public void shutdown() {
		this.running.set( false );
	}
}