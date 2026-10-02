import concurrent.futures,datetime,json,urllib.request,urllib.parse
import xml.etree.ElementTree as ET
from pathlib import Path
slugs=['smartbrainlib','geckolib','terrablender','structure-pool-api','lithostitched','more-hitboxes','owo-lib','cloth-config','yacl','resourceful-lib','cardinal-components-api','fabric-api','rei','jei','jade','modmenu']
def get(url):
    request=urllib.request.Request(url,headers={'User-Agent':'Fossil-native-port-library-audit/1.0'})
    with urllib.request.urlopen(request,timeout=25) as r:return json.load(r)
def check(slug):
    try:
        project=get('https://api.modrinth.com/v2/project/'+slug)
        versions={}
        for game in ['26.2','26.3']:
            query=urllib.parse.urlencode({'loaders':json.dumps(['fabric']),'game_versions':json.dumps([game])})
            files=get('https://api.modrinth.com/v2/project/'+slug+'/version?'+query)
            v=files[0] if files else None
            versions[game]=None if v is None else {'version':v['version_number'],'id':v['id'],'type':v['version_type'],'dependencies':v['dependencies']}
        return {'slug':slug,'source':project.get('source_url'),'description':project.get('description'),'downloads':project.get('downloads'),'versions':versions}
    except Exception as e:return {'slug':slug,'error':str(e)}
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool: results=list(pool.map(check,slugs))
portal_url='https://maven.kyrptonaught.dev/net/kyrptonaught/customportalapi/maven-metadata.xml'
try:
    with urllib.request.urlopen(portal_url,timeout=25) as response:
        portal_versions=[node.text for node in ET.fromstring(response.read()).findall('.//version')]
    portal={'source':'https://github.com/kyrptonaught/CustomPortalApi','metadata_url':portal_url,'published_versions':portal_versions,'target_versions':[v for v in portal_versions if '26.2' in v or '26.3' in v]}
except Exception as error:
    portal={'metadata_url':portal_url,'error':str(error)}
Path('docs/remaining-systems-library-audit.json').write_text(json.dumps({'checked':datetime.date.today().isoformat(),'loader':'fabric','projects':results,'custom_portal_api':portal,'notes':['Modrinth customportalapi belongs to a different Forge project; excluded.','No entity multipart project was identified by the guessed multipart-entities slug; this is not evidence that no library exists.','Published version metadata establishes availability, not tested interoperability.']},indent=2)+'\n',encoding='utf-8')
for r in results:print(json.dumps(r))
