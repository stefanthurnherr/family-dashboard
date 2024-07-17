# family-dashboard
Software package to build a digital picture frame based on the signal messenger. The original idea was to build a dashboard to incorporate other data (weather, calendar, etc), but so far only the picture frame functionality is implemented.


How To install OS and required packages
======
* A Raspberry Pi with an sd card, a display, internet connection and power supply
* Raspberry Pi OS (Desktop, bookworm) installed on the sd card
* This README assumes you are running on a 64-bit RPi. To check this:
  ```shell
  uname -m # should print aarch64
  dpkg --print-architecture # should print arm64
  ```
* Disable bluetooth (if not needed) by adding the line `dtoverlay=disable-bt` to /boot/firmware/config.txt
* Use `sudo nmtui` to configure additional wifi setups
* If you use the default pi user and absolutely don't want to change the default password, remove the warning:
  ```shell
  sudo apt purge libpam-chksshpwd pprompt
  ```
* Update to latest software using ```sudo apt update/upgrade```
* Install docker on the RPi:
  ```shell
  sudo apt install docker docker-compose
  ```
* On the RPi, add the ```pi``` user to the ```docker``` user group:
  ```shell
  sudo usermod -aG docker pi
  ```
* Reboot the RPi or re-login if you are logged in as user ```pi``` (to get the ```docker``` group applied)
* Try out some docker commmand to verify installation, e.g. ```docker run hello-world```  

Build and deploy the slideshow webapp
=======
* On any machine (not necessarily the RPi), clone this git repository
* Change into folder `slideshow-app`
* ```./mvnw package``` if you use maven and the maven-wrapper to build
* Transfer both the WAR file and the Dockerfile to your RPi
* ```docker build -t bondor/family-dashboard .``` to build the docker image and tag it
* ```docker container run --volume /home/pi/image-provider/fdimages:/opt/family-dashboard-images -p 8080:8080 -p 8443:8443 bondor/family-dashboard```

How to boot into Chromium kiosk mode
=======
* The default behaviour of RPi OS Desktop version is to auto-login and boot into Desktop, so the only thing we need to add is to auto-start Chromium in kiosk mode. To do this, add this line to `.config/wayfire.ini`:
  ```shell
  chromium = chromium-browser http://localhost:8080/slideshow --kiosk --noerrdialogs --disable-infobars --no-first-run --ozone-platform=wayland --enable-features=OverlayScrollbar --start-maximized
  ```

How to Install the signal-cli wrapper (docker container)
=======
* Copy `signal-cli-webserver` from this git repository to /home/pi/ and change directory into it
* If an older docker image for signal-cli-rest-api is around and you want to pull the latest released version: Run ```docker-compose pull```
* Start the docker container for the "signal-cli-rest-api": ```docker-compose -f docker-compose.yml up -d``` (uses the excellent Signal CLI REST API from [this github repo](https://github.com/bbernhard/signal-cli-rest-api/))
* Register a phone number (i.e. your home phone number): https://github.com/bbernhard/signal-cli-rest-api/blob/master/doc/EXAMPLES.md
    * The order of the API calls to execute seems to be as proposed here: https://github.com/bbernhard/signal-cli-rest-api/issues/441#issuecomment-1987086487
    * The captcha value supplied in the json payload can (must?) include the ```signalcaptcha://``` prefix.
    * I used my home phone number and it took several attempts (incl. captcha) with timeouts/"wrong captcha" before I got the call with the confirmation code. So keep cool and try again :-)
    * Use --data-binary "@data.txt" to specify request body data from textfile ./data.txt
* Change the phone number in signal-message-consumer/my.cfg

How to auto-start the docker containers above when the RPi (re-)boots:
=======
* Put the directory systemd-dashboard to /home/pi/
* Make sure that the bash script is executable: ```chmod +x service```
* Symlink the systemd service into /etc: ```sudo ln -s /home/pi/systemd-dashboard/dashboard-containers.service /etc/systemd/system/dashboard-containers.service```
* Enable the systemd service: ```sudo systemctl enable dashboard-containers.service```
* Reboot
* The systemd service should automatically start upon every (re-)boot. Use systemctl to start/stop the containers :-)

How to glue the whole thing together using crontab
======
```
# Reboot every night at 03:00
0 3 * * * sudo reboot

# Check for new Signal messages often
*/7 * * * * /home/pi/signal-message-consumer/receive-messages.py >> /home/pi/signal-message-consumer/cron.log 2>&1
```

Ideas for what to show (non-ordered)
======
* Personalized background images
* Shared calendar
* Weather
* Custom (welcome) message for guests
* Public transport timetable
