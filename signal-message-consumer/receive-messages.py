#!/usr/bin/python3
# -*- coding: utf-8 -*-

import requests
import json

import base64
import traceback
import os
import glob
import platform
import socket
import subprocess

import configparser

from datetime import datetime,timedelta

import re


BASE_FILEPATH = '/home/pi/signal-message-consumer/'
CONFIG_FILEPATH = BASE_FILEPATH + 'my.cfg'
RECEIVED_MESSAGES_LOG_FILE_PATH = BASE_FILEPATH + 'received-messages.txt'
VERSION = '0.31'
DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


class SignalApi(object):

    def __init__(self, url, number):
        self.url = url
        self.myNumber = number


    def about(self):
        # curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8095/v1/about'
        headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
        response = requests.get(self.url + '/v1/about', headers=headers)
        if (response and response.status_code == 200):
            return response.json()
        else:
            print('Got response code', response.status_code, ':', response.text)
            return None


    def receive_messages(self):
        # curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/receive/<number>'
        headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
        response = requests.get(self.url + '/v1/receive/' + self.myNumber, headers=headers)
        if (response and response.status_code == 200):
            return response.json()
        else:
            print('Got response code', response.status_code, ':', response.text)
            return None


    def list_attachments(self):
        # curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/attachments'
        headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
        response = requests.get(self.url + '/v1/attachments/', headers=headers)
        if (response and response.status_code == 200):
            return response.json()
        else:
            print('Got response code', response.status_code, ':', response.text)
            return None


    def get_attachment_binary(self, attachmentId, delete = False):
        headers = {} # {'Content-type': 'application/json', 'Accept': 'text/plain'}
        response = requests.get(self.url + '/v1/attachments/' + attachmentId, headers=headers)
        if ( not response or response.status_code != 200):
            return None

        if (delete):
            self.delete_attachment(attachmentId)

        return response.content


    def delete_attachment(self, attachmentId):
        # curl -X DELETE -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/attachments/<id>'
        headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
        response = requests.delete(self.url + '/v1/attachments/' + attachmentId, headers=headers)
        if (response and response.status_code == 204):
            return True
        else:
            print('Got response code', response.status_code, ':', response.text)
            return False


    def sendMessage(self, recipientNumber, message):
        headers = {'Content-type': 'application/json'}
        payload = {'message': message, 'number': self.myNumber, 'recipients': [recipientNumber]}
        response = requests.post(self.url + '/v2/send', json=payload, headers=headers)
        if (response and response.status_code == 201):
            return response.json()
        else:
            print('Got response code', response.status_code, ':', response.text)
            return None

#
# ################# End of class SignalApi
#

def get_uptime_seconds():
    with open('/proc/uptime', 'r') as f:
        uptime_seconds = float(f.readline().split()[0])
    return uptime_seconds


def get_memory_total_str():
    mem_bytes = os.sysconf('SC_PAGE_SIZE') * os.sysconf('SC_PHYS_PAGES')
    mem_mb = mem_bytes/(1024.**2)
    return str(round(mem_mb)) + ' MB'


def get_docker_version():
    result = subprocess.run(['docker', '--version'], capture_output=True, encoding='UTF-8')
    # Example stdout: Docker version 20.10.19, build d85ef84

    stdoutString = result.stdout
    return stdoutString.strip()[15:]


def get_my_ip_address():
    hostname = socket.gethostname()
    sockets = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    ip_string = (([ip for ip in socket.gethostbyname_ex(hostname)[2] if not ip.startswith("127.")] or [[(s.connect(("8.8.8.8", 53)), s.getsockname()[0], s.close()) for s in [sockets]][0][1]]) + ["(no IP found)"])[0]
    return ip_string


def downloadAndSaveAttachment(attachmentId, contentType, targetFolderPath, signalApi):
    attachmentFilePath = targetFolderPath + attachmentId
    with open(attachmentFilePath, 'wb') as attachmentFile:
        attachmentFile.write(signalApi.get_attachment_binary(attachmentId, True))
    return attachmentFilePath


def purgeOldestAttachmentsIfTooMany(targetFolderPath, maxKeepCount):
    attachments = glob.glob(targetFolderPath + '*.jpg', recursive=False)
    tooManyCount = len(attachments) - maxKeepCount

    if (tooManyCount > 0):
        print('Too many attachments, max count exceeded by', tooManyCount)
        attachments.sort(key=os.path.getmtime)
        for attachment in attachments[0:tooManyCount]:
            print('  Deleting', attachment)
            os.remove(attachment)


def processMessageCommand(command, sourceNumber, signalApi, targetFolderPath):
    if (m := re.match('^keep ([0-9]*)$', command)):

        keepCount = int(m.group(1))
        print('keepCount parsed: ' + str(keepCount))

        dirEntryList = []
        for dirEntry in os.scandir(targetFolderPath):
            if (dirEntry.is_file()):
                dirEntryList.append(dirEntry)

        dirEntryList = sorted(dirEntryList, key=lambda x: x.stat().st_ctime)
        deleteDirEntryList = dirEntryList[keepCount:]

        for dirEntry in dirEntryList:
            print(dirEntry.name + ' was created at ' + str(dirEntry.stat().st_ctime))

        return True

    elif (m := re.match('^status$', command)):
        statusMessage = 'Hi, here is my status:\n'
        print('Responding to "status" command...')

        statusMessage += '  System time: ' + datetime.now().strftime(DATETIME_FORMAT) + '\n'

        statusMessage += '  Uptime: ' + str(timedelta(seconds=get_uptime_seconds())) + '\n'

        statusMessage += '  OS: ' + platform.platform() + '\n'

        statusMessage += '  IP address: ' + get_my_ip_address() + '\n'
        
        statusMessage += '  Total memory: ' + get_memory_total_str() + '\n'

        statusMessage += '  Python version: ' + platform.python_version()+ '\n'

        statusMessage += '  receive-messages version: ' + VERSION + '\n'

        statusMessage += '  Docker version: ' + get_docker_version() + '\n'

        aboutSignalCli = signalApi.about()
        statusMessage += '  signal-cli version: ' + aboutSignalCli['version'] + '\n'

        print(statusMessage)
        sendOk = signalApi.sendMessage(sourceNumber, statusMessage)
        return bool(sendOk)

    print('Unknown command "' + command + '", ignoring.')
    return False


if __name__ == "__main__":

    config = configparser.ConfigParser()
    config.read(CONFIG_FILEPATH)
    signalApi = SignalApi(config['SignalConfig']['rest_api_url'],
                            config['SignalConfig']['my_number'])

    now_string = datetime.now().strftime(DATETIME_FORMAT)
    print("{} Fetching Signal messages...".format(now_string))

    with open(RECEIVED_MESSAGES_LOG_FILE_PATH, 'a') as messagesFile:
        try:
            messagesFile.write("# running v{} at {}\n".format(VERSION, now_string))

            attachmentList = signalApi.list_attachments()
            if attachmentList is None:
                messagesFile.write("  currently no downloadable attachments are available.\n")
            else:
                messagesFile.write("  currently downloaded {} attachments: {}\n".format(len(attachmentList), attachmentList))
            
            targetFolderPath = config['MediaStorage']['images_folder_path']

            messages = signalApi.receive_messages()
            if (messages):
                for message in messages:
                    messagesFile.write(json.dumps(message, indent=4))
                    messagesFile.write('\n')

                    if ('typingMessage' in message['envelope']):
                        # "Typing message" state changed - ignore these messages
                        continue

                    if ('receiptMessage' in message['envelope']):
                        # 'Read receipt' message received - ignore these messages
                        continue

                    senderName = message['envelope']['sourceName']
                    sourceNumber = message['envelope']['sourceNumber']
                    dataMessage = message['envelope']['dataMessage']
                    messageTextRaw = dataMessage['message']
                    messageText = messageTextRaw if messageTextRaw else '(no message)'
                    attachments = dataMessage.get('attachments', [])
                    messagesFile.write("{} says \"{}\" and has sent {} attachments.".format(senderName, messageText, len(attachments)))
                    messagesFile.write('\n')

                    if (messageText.startswith('/')):
                        commandRecognized = processMessageCommand(messageText[1:], sourceNumber, signalApi, targetFolderPath)
                        messagesFile.write('  Message was recognized as command? {}'.format(commandRecognized))
                        messagesFile.write('\n')

                    else:
                        for attachment in attachments:
                            attachmentId = attachment['id']
                            attachmentContentType = attachment['contentType']
                            attachmentFilePath = downloadAndSaveAttachment(attachmentId, attachmentContentType, targetFolderPath, signalApi)
                            messagesFile.write(" Found attachment {} of type {}, saved as {}".format(attachmentId, attachmentContentType, attachmentFilePath))
                            messagesFile.write('\n')

                        messagesFile.write('\n')

                        if (attachments):
                            purgeOldestAttachmentsIfTooMany(targetFolderPath, config.getint('MediaStorage', 'images_keep_max_count'))


            messagesFile.write('# done with all messages.\n')

        except KeyError as e:
            messagesFile.write('Got KeyError exception:')
            messagesFile.write(traceback.format_exc())
            messagesFile.write('\n')

        print("{} done.".format(datetime.now().strftime(DATETIME_FORMAT)))
