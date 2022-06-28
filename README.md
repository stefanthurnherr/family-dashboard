# family-dashboard
Dashboard for the whole family shown at home


How To Set Up an RPi in Chromium kiosk mode
======
* A Raspberry Pi with an sd card, a display, internet connection and power supply
* Use the excellent [chilipie-kiosk](https://github.com/jareware/chilipie-kiosk/) RPi image to boot directly into full-screen Chrome
* Update to latest software using ```apt-get update/upgrade```
* Install docker on the RPi:
```shell
  curl -fsSL https://get.docker.com -o get-docker.sh
  sh get-docker.sh
```
* On the RPi, add the ```pi``` user to the ```docker``` user group: ```sudo usermod -aG docker pi```
* Try out some docker commmand to verify installation, e.g. ```docker ps```  
* Build your webapp and create a Dockerfile (I followed [this tutorial](https://medium.com/swlh/how-to-run-spring-boot-application-on-raspberry-pi-using-docker-d633e15ffff2))
    * ```./mvnw package``` if you use maven and the maven-wrapper to build
    * Transfer both the WAR file and the Dockerfile to your RPi
* ```docker build -t bondor/family-dashboard .``` to build the docker image and tag it
* ```docker container run --volume /home/pi/fdimages:/opt/family-dashboard-images -p 8080:8080 --restart unless-stopped bondor/family-dashboard &```
* Set the URL for Chromium by creating the file ```/home/pi/chilipie_url.txt``` with url ```http://localhost:8080``` as its first and only line.

How to Install the signal-cli wrapper (docker container)
=======
* Install docker-compose (based on [these instructions](https://www.upswift.io/post/install-docker-compose-on-raspberry-pi)):
    * ```sudo apt-get install libffi-dev libssl-dev```
    * ```sudo apt install python3-dev```
    * ```sudo apt-get install -y python3 python3-pip```
    * ```sudo pip3 install docker-compose```
* Install the signal-cli wrapper: https://github.com/bbernhard/signal-cli-rest-api/
* Start the docker container: ```docker-compose -f docker-compose.yml up -d```
* Register a phone number (i.e. your home phone number): https://github.com/bbernhard/signal-cli-rest-api/blob/master/doc/EXAMPLES.md
    * I used my home phone number and it took several attempts (incl. captcha) with timeouts/"wrong captcha" before I got the call with the confirmation code. So keep cool and try again :-)

How to auto-start the docker containers above when the RPi (re-)boots:
=======
* Put the directory systemd-dashboard to /home/pi/
* Make sure that the bash script is executable: ```chmod +x service```
* Symlink the systemd service into /etc: ```sudo ln -s /home/pi/systemd-dashboard/dashboard-containers.service /etc/systemd/system/dashboard-containers.service```
* Use systemctl to start/stop the containers :-)

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
