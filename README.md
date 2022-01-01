# family-dashboard
Dashboard for the whole family shown at home


How To Get Started
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
* Build your webapp and Dockerfile (I followed [this tutorial](https://medium.com/swlh/how-to-run-spring-boot-application-on-raspberry-pi-using-docker-d633e15ffff2)) and transfer both (WAR file and Dockerfile) to the RPi
    * ```./mvnw package``` if you use maven and the maven-wrapper to build
* ```docker build -t bondor/family-dashboard .``` to build the docker image and tag it
* ```docker container run -p 8080:8080 --restart unless-stopped bondor/family-dashboard```
* Set the URL for Chromium by creating the file ```/home/pi/chilipie_url.txt``` with url ```http://localhost:8080``` as its first and only line.

Ideas for what to show (non-ordered)
======
* Shared calendar
* Weather
* Custom (welcome) message for guests
* Public transport timetable
