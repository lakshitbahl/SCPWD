/**
 * Solar Panel Monitor
 *
 * Uses current sensor to measure output of solar panels.
 * Optional additional measurement of luminosity.
 * Optional additional measurement of in/output to electricity grid
 *
 *@author Bernd Giesecke
 *@version 0.2 beta August 19, 2015.
 */

/**
 * Solar Panel Monitor
 * loop
 * required by Arduino IDE
 *
 * Main program loop
 */
void loop() {
  /** Actual time in milliseconds since start of spMonitor */
  unsigned long now = millis();
  if ( now - lastMeasure >= measureFreq ) { /* initiate measurement every 1 seconds */
    lastMeasure = now;
    wdt_reset();

    /* Activity LED on */
    digitalWrite ( activityLED, HIGH );

    wdt_reset();
    /* Get the light measurement if a sensor is attached */
    readLux();

    /* Get the measured current from the solar panel */
    getCTValues(0);

    /** Get the measured current from mains */
    getCTValues(1);

    /* Activity LED off */
    digitalWrite ( activityLED, LOW );
  }

  if ( now - lastSave >= 60000 ) { /* Save data every minute */
    lastSave = now;
    wdt_reset();
    saveData();
  }

  //if ( now - lastReset >= 600000 ) { /* Reset every hour */
  /* Wait for watchdog reset */
  //  wdt_disable();
  //  wdt_enable(WDTO_15MS);
  //  delay (500);
  //}

  /** Get clients coming from server */
  client = server.accept();

  /* There is a new client? */
  if ( client.available() ) {
    wdt_reset();
    /** Character holding the command that was sent */
    char command = client.read();

    /** Only for claibration needed */
    //if ( command == 'c' ) { /* Set the CT calibration value e.g. c16.060606 => value 6.060606 for sensor 1 */
    //  command = client.read(); /* get the sensor number */
    /** Calibration factor that was sent */
    //  double readCal = client.parseFloat();

    //  if ( command == '1' ) {
    //    iCal[0] = readCal;
    //    emon[0].current ( 0, iCal[0] );
    //  }

    //  if ( command == '2' ) {
    //    iCal[1] = readCal;
    //    emon[1].current ( 1, iCal[1] );
    //  }

    //  client.println ( readCal, 6 );
    //} else
    if ( command == 'e' ) { /* Get the actual settings */
      client.print ( "F " + String ( measureFreq ) + "s" );
      client.println ( " V " + String ( vCal ) );
      client.print ( "C1 " + String ( iCal1, 1 ) );
      client.println ( " C2 " + String ( iCal2, 1 ) );
    }
    else if ( command == 'b' ) { /* Backup database */
      /** Instance to Linino process */
      Process sqLite;
      sqLite.runShellCommand ( "rm /mnt/sda1/s.bu.gz" );
      sqLite.runShellCommand ( "echo '.dump' | sqlite3 /mnt/sda1/s.db | gzip -c >/mnt/sda1/s.bu.gz" );
    }
    else if ( command == 'r' ) { /* restore database */
      /** Instance to Linino process */
      Process sqLite;
      sqLite.runShellCommand ( "rm /mnt/sda1/bu.db" );
      sqLite.runShellCommand ( "zcat /mnt/sda1/s.bu.gz | sqlite3 /mnt/sda1/bu.db" );
    }

    /* Close connection and free resources. */
    wdt_reset();
    client.flush();
    client.stop();
  }
}

