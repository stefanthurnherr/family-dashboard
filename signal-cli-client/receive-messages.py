#!/usr/bin/python3
# -*- coding: utf-8 -*-

import requests
import json

from datetime import datetime


DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def receive_messages():
    # curl -X GET -H "Content-Type: application/json" 'http://127.0.0.1:8080/v1/receive/<number>'
    headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
    response = requests.get('http://127.0.0.1:8095/v1/receive/<number>', headers=headers)
    json_data = response.json() if response and response.status_code == 200 else None
    return json_data


if __name__ == "__main__":

    now_string = datetime.now().strftime(DATETIME_FORMAT)

    with open('received-messages.txt', 'a') as messagesFile:

        messagesFile.write("# running at {}\n".format(now_string))

        messages = receive_messages()
        print(messages)
        if (messages):
            for message in messages:
                messagesFile.write(json.dumps(message))
                messagesFile.write('\n')
