#!/usr/bin/python3
# -*- coding: utf-8 -*-

import requests
import json

import base64
import mimetypes

from datetime import datetime

import re


SIGNAL_API_URL = 'http://127.0.0.1:8095'
SIGNAL_PHONE_NUMBER = '+46317132834'
ATTACHMENTS_FOLDER_PATH = '/home/pi/image-provider/fdimages/'

VERSION = '0.20'

DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def receive_messages():
    # curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/receive/<number>'
    headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
    response = requests.get(SIGNAL_API_URL + '/v1/receive/' + SIGNAL_PHONE_NUMBER, headers=headers)
    json_data = response.json() if response and response.status_code == 200 else None
    return json_data

def list_attachments():
    # curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/attachments'
    headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
    response = requests.get(SIGNAL_API_URL + '/v1/attachments/', headers=headers)
    json_data = response.json() if response and response.status_code == 200 else None
    return json_data

def get_attachment_binary(attachmentId, delete = False):
    # curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/attachments'
    headers = {} # {'Content-type': 'application/json', 'Accept': 'text/plain'}
    response = requests.get(SIGNAL_API_URL + '/v1/attachments/' + attachmentId, headers=headers)
    if ( not response or response.status_code != 200):
        return None

    if (delete):
        delete_attachment(attachmentId)

    return response.content

def delete_attachment(attachmentId):
    # curl -X DELETE -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/attachments/<id>'
    headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
    response = requests.delete(SIGNAL_API_URL + '/v1/attachments/' + attachmentId, headers=headers)
    return (response and response.status_code == 200)


def downloadAndSaveAttachment(attachmentId, contentType):
    fileExtension = mimetypes.guess_extension(attachment['contentType'])
    attachmentFilePath = ATTACHMENTS_FOLDER_PATH + attachmentId + fileExtension
    with open(attachmentFilePath, 'wb') as attachmentFile:
        attachmentFile.write(get_attachment_binary(attachmentId, True))
    return attachmentFilePath

def processMessageCommand(command):
    if (re.match(r'^keep ([0-9]*)$', command)):
       return True

    return False 


if __name__ == "__main__":

    now_string = datetime.now().strftime(DATETIME_FORMAT)
    print("{} Fetching Signal messages...".format(now_string))

    with open('/home/pi/signal-client/received-messages.txt', 'a') as messagesFile:

        messagesFile.write("# running v{} at {}\n".format(VERSION, now_string))

        attachmentList = list_attachments()
        messagesFile.write("  currently downloaded {} attachments: {}\n".format(len(attachmentList), attachmentList))

        messages = receive_messages()
        if (messages):
            for message in messages:
                messagesFile.write(json.dumps(message, indent=4))
                messagesFile.write('\n')

                senderName = message['envelope']['sourceName']
                messageTextRaw = message['envelope']['dataMessage']['message']
                messageText = messageTextRaw if messageTextRaw else '(no message)'
                attachments = message['envelope']['dataMessage']['attachments']
                messagesFile.write("{} says \"{}\" and has sent {} attachments.".format(senderName, messageText, len(attachments)))
                messagesFile.write('\n')

                if (messageText.startswith('/')):
                    commandRecognized = processMessageCommand(messageText[1:])
                    messagesFile.write('  Message was recognized as command? {}'.format(commandRecognized))
                    messagesFile.write('\n')

                else:
                    for attachment in attachments:
                        attachmentId = attachment['id']
                        attachmentContentType = attachment['contentType']
                        attachmentFilePath = downloadAndSaveAttachment(attachmentId, attachmentContentType)
                        messagesFile.write(" Found attachment {} of type {}, saved as {}".format(attachmentId, attachmentContentType, attachmentFilePath))
                        messagesFile.write('\n')

            messagesFile.write('\n')

        messagesFile.write('# done with all messages.\n')

    print("{} done.".format(datetime.now().strftime(DATETIME_FORMAT)))


