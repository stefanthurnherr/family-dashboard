# family-dashboard
Dashboard for the whole family shown at home


How To Get Started
======
* A Raspberry Pi with a display, internet connection and power supply
* Use the excellent [chilipie-kiosk](https://github.com/jareware/chilipie-kiosk/) RPi image to boot directly into full-screen Chrome
* Update to latest software using apt-get update/upgrade
* Install docker on the RPi:
```shell
  curl -fsSL https://get.docker.com -o get-docker.sh
  sh get-docker.sh
```
* On the RPi, add the pi user to the docker user group: ```sudo usermod -aG docker pi```
* Try out some docker commmand, e.g. ``docker ps```  
* Build your webapp and Dockerfile (I followed [this tutorial](https://medium.com/swlh/how-to-run-spring-boot-application-on-raspberry-pi-using-docker-d633e15ffff2)) and transfer it to the RPi
* docker container run -p 8080:8080 --restart unless-stopped bondor/dashboard
* Set the URL for Chromium by creating the file ```/home/pi/chilipie_url.txt``` with url ```http://localhost:8080``` as its first and only line.
