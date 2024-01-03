# family-dashboard
Dashboard for the whole family shown at home


How To Set Up an RPi in Chromium kiosk mode
======
* A Raspberry Pi with an sd card, a display, internet connection and power supply
* Raspberry Pi OS (Desktop, bookworm) installed on the sd card
* This README assumes you are running on a 64-bit RPi. To check this:
  ```shell
  uname -m # should print aarch64
  dpkg --print-architecture # should print arm64
  ```
* Disable bluetooth (if not needed) by adding the line `dtoverlay=disable-bt` to /boot/config.txt
* If you use the default pi user and absolutely don't want to change the default password, remove the warning:
  ```shell
  sudo apt purge libpam-chksshpwd pprompt
  ```
* Update to latest software using ```apt-get update/upgrade```
* Install docker on the RPi:
  ```shell
  sudo apt install docker
  ```
* On the RPi, add the ```pi``` user to the ```docker``` user group:
  ```shell
  sudo usermod -aG docker pi
  ```
* Reboot the RPi or re-login if you are logged in as user ```pi``` (to get the ```docker``` group applied)
* Try out some docker commmand to verify installation, e.g. ```docker run hello-world```  
* Build your webapp and create a Dockerfile (I followed [this tutorial](https://medium.com/swlh/how-to-run-spring-boot-application-on-raspberry-pi-using-docker-d633e15ffff2))
    * ```./mvnw package``` if you use maven and the maven-wrapper to build
    * Transfer both the WAR file and the Dockerfile to your RPi
* ```docker build -t bondor/family-dashboard .``` to build the docker image and tag it
* ```docker container run --volume /home/pi/image-provider/fdimages:/opt/family-dashboard-images -p 8080:8080 -p 8443:8443 bondor/family-dashboard```
* Set the URL for Chromium by creating the file ```/home/pi/chilipie_url.txt``` with url ```http://localhost:8080/slideshow``` as its first and only line.

How to boot into Chromium kiosk mode
=======
* The default behaviour of RPi OS Desktop version is to auto-login and boot into Desktop, so the only thing we need to add is to auto-start Chromium in kiosk mode
* Add this to `.config/wayfire.ini` (the url will be changed later):
  ```shell
  chromium = chromium-browser https://time.is/London --kiosk --noerrdialogs --disable-infobars --no-first-run --ozone-platform=wayland --enable-features=OverlayScrollbar --start-maximized
  ```

How to Install the signal-cli wrapper (docker container)
=======
* Install docker-compose (based on [these instructions](https://www.upswift.io/post/install-docker-compose-on-raspberry-pi)):
    * ```sudo apt-get install libffi-dev libssl-dev```
    * ```sudo apt-get install python3-dev```
    * ```sudo apt-get install -y python3 python3-pip```
    * ```sudo pip3 install docker-compose```
* Start the docker container for the "signal-cli-rest-api": ```docker-compose -f docker-compose.yml up -d``` (uses the excellent Signal CLI REST API from [this github repo](https://github.com/bbernhard/signal-cli-rest-api/))
* Register a phone number (i.e. your home phone number): https://github.com/bbernhard/signal-cli-rest-api/blob/master/doc/EXAMPLES.md
    * I used my home phone number and it took several attempts (incl. captcha) with timeouts/"wrong captcha" before I got the call with the confirmation code. So keep cool and try again :-)
    * Use --data-binary "@data.txt" to specify request body data from textfile ./data.txt
* Change the phone number in receive-messages.py (constant defined at the top)

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
*/3 * * * * /home/pi/signal-message-consumer/receive-messages.py >> /home/pi/signal-message-consumer/cron.log 2>&1

```

How to enable sending images to the Rpi through signal
======
* Create a crontab script that periodically calls Signal's /receive API and puts the received images into the dashboard's fdimages folder
    * ```curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/receive/<number>'```

How to reset docker if the docker daemon doesn't start anymore
======
* sudo apt-get purge docker-ce docker-ce-cli
* sudo rm -rf /var/lib/docker
* sudo shutdown -r now
* sudo apt-get install docker-ce
* docker ps :-)

Ideas for what to show (non-ordered)
======
* Personalized background images
* Shared calendar
* Weather
* Custom (welcome) message for guests
* Public transport timetable
