#!/usr/bin/python3
# -*- coding: utf-8 -*-

import requests
import json

import base64
import mimetypes
import traceback
import os
import glob

from datetime import datetime

import re


SIGNAL_API_URL = 'http://127.0.0.1:8095'
SIGNAL_PHONE_NUMBER = '+46987654321'

RECEIVED_MESSAGES_LOG_FILE_PATH = '/home/pi/signal-client/received-messages.txt'

ATTACHMENTS_FOLDER_PATH = '/home/pi/image-provider/fdimages/'
ATTACHMENTS_KEEP_MAX_COUNT = 20

VERSION = '0.22'

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

def purgeOldestAttachmentsIfTooMany():
    # attachments = [f for f in os.listdir(ATTACHMENTS_FOLDER_PATH)]
    attachments = glob.glob(ATTACHMENTS_FOLDER_PATH + '*.jpg', recursive=False)
    tooManyCount = len(attachments) - ATTACHMENTS_KEEP_MAX_COUNT
    
    if (tooManyCount > 0):
        print('Too many attachments, max count exceeded by', tooManyCount)
        attachments.sort(key=os.path.getmtime)
        for attachment in attachments[0:tooManyCount]:
            print('  Deleting', attachment)
            os.remove(attachment)


def processMessageCommand(command):
    if (m := re.match(r'^keep ([0-9]*)$', command)):

        keepCount = int(m.group(1))
        print('keepCount parsed: ' + str(keepCount))

        dirEntryList = []
        for dirEntry in os.scandir(ATTACHMENTS_FOLDER_PATH):
            if (dirEntry.is_file()):
                dirEntryList.append(dirEntry)

        dirEntryList = sorted(dirEntryList, key=lambda x: x.stat().st_ctime)
        deleteDirEntryList = dirEntryList[keepCount:]

        for dirEntry in dirEntryList:
            print(dirEntry.name + ' was created at ' + str(dirEntry.stat().st_ctime))

        return True

    return False 


if __name__ == "__main__":

    now_string = datetime.now().strftime(DATETIME_FORMAT)
    print("{} Fetching Signal messages...".format(now_string))

    with open(RECEIVED_MESSAGES_LOG_FILE_PATH, 'a') as messagesFile:
        try:
            messagesFile.write("# running v{} at {}\n".format(VERSION, now_string))

            attachmentList = list_attachments()
            messagesFile.write("  currently downloaded {} attachments: {}\n".format(len(attachmentList), attachmentList))

            messages = receive_messages()
            if (messages):
                for message in messages:
                    messagesFile.write(json.dumps(message, indent=4))
                    messagesFile.write('\n')

                    if ('typingMessage' in message['envelope']):
                        # "Typing message" state changed - ignore these messages 
                        continue


                    senderName = message['envelope']['sourceName']
                    dataMessage = message['envelope']['dataMessage']
                    messageTextRaw = dataMessage['message']
                    messageText = messageTextRaw if messageTextRaw else '(no message)'
                    attachments = dataMessage.get('attachments', [])
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
                        
                        if (attachments):    
                            purgeOldAttachmentsIfTooMany()


            messagesFile.write('# done with all messages.\n')

        except KeyError as e:
            messagesFile.write('Got KeyError exception:')
            messagesFile.write(traceback.format_exc())
            messagesFile.write('\n')

        print("{} done.".format(datetime.now().strftime(DATETIME_FORMAT)))



