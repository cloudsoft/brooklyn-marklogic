import sys

try:
    import requests
except ImportError:
    print "Couldn't import requests. Have you run `sudo pip install requests`?"
    sys.exit(1)

def loadApps(entities=[]):
    payload = {'items': ','.join(entities)}
    return requests.get('http://localhost:8081/v1/applications/fetch', params=payload).json()

def loadEntitySensors(entity):
    appId = entity['applicationId']
    entId = entity['id']
    return requests.get('http://localhost:8081/v1/applications/'+appId+'/entities/'+entId+'/sensors/current-state').json()

def isMarkLogicEntity(app):
    return 'MarkLogic' in app['name']

def isMarkLogicNode(app):
    return app['type'] == 'io.cloudsoft.marklogic.nodes.MarkLogicNode'

def walkEntities():
    """Repeatedly calls loadApps until no new children are encountered"""
    toCheck = ['']
    visited = set()
    
    while len(toCheck) > 0:
        checking = filter(lambda appId: appId not in visited, toCheck) 
        checked  = loadApps(checking)
        toCheck = []
        for newApp in checked:
            if newApp['id'] not in visited:
                yield newApp
                visited.add(newApp['id'])
                if 'children' in newApp:
                    for child in newApp['children']:
                        toCheck.append(child['id'])


for entity in walkEntities():
    # print entity
    if isMarkLogicNode(entity):
        host = loadEntitySensors(entity)['host.name']
        if host:
            print host

