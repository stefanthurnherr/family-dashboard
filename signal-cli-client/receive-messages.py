#!/usr/bin/python3
# -*- coding: utf-8 -*-

import requests
import json

import base64
import mimetypes

from datetime import datetime


SIGNAL_API_URL = 'http://127.0.0.1:8095'
SIGNAL_PHONE_NUMBER = '<number>'

VERSION = '0.19'

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



if __name__ == "__main__":

    now_string = datetime.now().strftime(DATETIME_FORMAT)

    with open('/home/pi/signal-client/received-messages.txt', 'a') as messagesFile:

        messagesFile.write("# running v{} at {}\n".format(VERSION, now_string))

        attachmentList = list_attachments()
        messagesFile.write("  currently downloaded {} attachments: {}\n".format(len(attachmentList), attachmentList))

        messages = receive_messages()
        print(messages)
        if (messages):
            for message in messages:
                messagesFile.write(json.dumps(message, indent=4))
                messagesFile.write('\n')

                senderName = message['envelope']['sourceName']
                messageText = message['envelope']['dataMessage']['message']
                attachments = message['envelope']['dataMessage']['attachments']
                messagesFile.write("{} says \"{}\" and has sent {} attachments.".format(senderName, messageText, len(attachments)))
                messagesFile.write('\n')

                for attachment in attachments:
                    attachmentId = attachment['id']
                    fileExtension = mimetypes.guess_extension(attachment['contentType'])
                    #attachmentBytes = base64.decodebytes(attachment)
                    attachmentFilePath = '/home/pi/signal-client/attachments/' + attachmentId + fileExtension
                    with open(attachmentFilePath, 'wb') as attachmentFile:
                        attachmentFile.write(get_attachment_binary(attachmentId, True))

                    messagesFile.write(" Found attachment {}, saved as {}".format(attachmentId + fileExtension, attachmentFilePath))
                    messagesFile.write('\n')


            messagesFile.write('\n')

        messagesFile.write('# done with all messages.\n')
