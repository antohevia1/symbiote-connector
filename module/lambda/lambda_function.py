
import time
import json
import boto3
import hashlib
import os

ssm = boto3.client("ssm")
connection_params={}
user_params = {}

connection_params['instanceId'] =  os. environ['INSTANCE_ID'].split("/")[1]

#volume name on the given ec2-instance
vol_name = 'websocket_state'
path_to_db='/var/lib/docker/volumes/'+vol_name+'/_data/websockets.db'
##path_to_db='/var/lib/docker/volumes/my-vol/_data/websockets.db'
# Need to have image available on the EC2 instance
image_name = 'websocket'




def get_hash(connection_params):
    connection_exclude={x: connection_params[x] for x in connection_params if x not in {"instanceId"}}
    return hashlib.sha1(''.join(connection_exclude.values()).encode('utf-8')).hexdigest()

def send_command(command, response=False):
    response = ssm.send_command(
                InstanceIds=[connection_params['instanceId']],
                DocumentName="AWS-RunShellScript",
                Parameters={
                    "commands": [command]
                },  # replace command_to_be_executed with command
            )
    if not response:
        return 'Command executed'
        # fetching command id for the output
    else:
        command_id = response["Command"]["CommandId"]
        time.sleep(3)
        return ssm.get_command_invocation(CommandId=command_id, InstanceId=connection_params['instanceId'])
    
    
def get_state(container_id):
    
    """ Gets the state of the given connection. It can either be 0,1 or an empty string """

    command = "sqlite3 "+path_to_db+" 'select is_active from websockets where websocket_id =\""+container_id+"\"'"
    return {'body': send_command(command, response=True)['StandardOutputContent'].strip(), 'Status': 'Success'}


def remove_socket(container_id):
    
    """ stop the container running that socket """

    command_1 = 'docker stop '+container_id
    send_command(command_1)
    
    #update the websockets table in sqlite3
    command_2 = "sqlite3 "+path_to_db+" 'update websockets set is_active=0 where websocket_id=\""+container_id+"\"'"
    send_command(command_2)
    return {'body':'Websocket closed', 'Status': 'Success'}
    
 
def docker_run_command(websocket_id):
    
    """returns a string with the command that should be run to create the docker container
        so the websocket is created for the given parameters.
        Note that the image has to be already built """
    
    return  """docker run -itd --rm -v {}:/home/ec2-user/vol --name {} \
                -e user='{}' \
                -e pass='{}' \
                -e internalID='{}' \
                -e interWorkingInterface='{}' \
                -e platformID='{}' \
                -e websocketId='{}' \
                {} """.format(vol_name,
                                websocket_id,
                                user_params['asapa_email'],
                                user_params['asapa_pass'] ,
                                connection_params['internalID'],
                                connection_params['interWorkingInterface'],
                                connection_params['platformID'],
                                websocket_id, 
                                image_name) 

def open_socket(state, websocket_id):

    """ it creates a websocket by running a container with the given interpace, resource and platform parameter.
        If the container exists and is running it will do nothing
        if exists and is not running it will do docker start
        if its a new connection it will create a new container by docker run """
    if(state=='1'):
        return {'body':'Do nothing', 'Status': 'Success'}
    else:
        send_command("sqlite3  "+path_to_db+" 'insert into websockets VALUES(\""+websocket_id+"\", 0 , 0, 0,0)'")
        print('building new container for connection: '+ websocket_id)
        return  send_command(docker_run_command( websocket_id),response=True)
        
        
def respond(err, res=None):
    return {
        'statusCode': '400' if err else '200',
        'body': json.dumps(res),
        'headers': {
            'Content-Type': 'application/json',
        },
    }
    
def lambda_handler(event, context):

    #check if the input is correct
    try:
        user_params['asapa_email']  = event['headers']['asapa_email']
        user_params['asapa_pass']  = event['headers']['asapa_pass']
        connection_params['internalID']  = event['headers']['internalID']
        connection_params['interWorkingInterface']  = event['headers']['interWorkingInterface']
        connection_params['platformID']  = event['headers']['platformID']

    except:
         print("Invalid user input")
         return respond(ValueError('Invaid user input'))
      
    
    websocket_id = get_hash(connection_params)
    state = get_state(websocket_id)['body']

    operations = {
        'DELETE':   lambda remove_socket, state, websocket_id:  remove_socket(websocket_id),
        'POST':     lambda open_socket, state, websocket_id:    open_socket(state, websocket_id),
        'GET' :     lambda get_socket_status, state, websocket_id:    get_state(websocket_id),
    }

    operation = event['httpMethod']
    if operation in operations:
        function = open_socket if operation == 'POST' else get_state if operation =='GET'else remove_socket
        response = operations[operation](function, state,  websocket_id)        
        err = True if response['Status']== "Failed" else False
        return respond(err, response)
    else:
        return respond(ValueError('Unsupported method "{}"'.format(operation)))