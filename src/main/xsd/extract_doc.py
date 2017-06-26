# import xml.etree.ElementTree as ET
from lxml import etree as ET
import sys
import os
import pprint
import re
import json
import codecs
from enum import Rename
import datetime



def parseElements(filename, namespace, types) :
	ET.register_namespace("ms","http://www.meta-share.org/OMTD-SHARE_XMLSchema")
	ET.register_namespace("xs","http://www.w3.org/2001/XMLSchema")
	# ET.register_namespace("","http://www.meta-share.org/OMTD-SHARE_XMLSchema")
	tree = ET.parse(filename)
	root = tree.getroot()
	nodes = root.xpath(".//xs:documentation/../..",namespaces=namespace)
	for node in nodes :
		if 'name' in node.attrib:
			nodeName = node.attrib['name']
			


			thisDesLabel = {}

			minOccurs = 1
			if 'minOccurs' in node.attrib:
				minOccurs = node.attrib['minOccurs']
			if minOccurs == 1:
				thisDesLabel['mandatory'] = 'true'
			else: 
				thisDesLabel['mandatory'] = 'false'

			doc = node.xpath('.//xs:documentation',namespaces=namespace)
			thisDesLabel['desc'] = doc[0].text

			label = node.xpath('.//label',namespaces=namespace)
			if len(label) > 0 :
				thisDesLabel['label'] = label[0].text
			else:
				thisDesLabel['label'] = None

			recommended = node.xpath('.//recommended',namespaces=namespace)
			if len(recommended) > 0 :
				thisDesLabel['recommended'] = recommended[0].text
			else:
				thisDesLabel['recommended'] = None


			if nodeName in types :
				if types[nodeName]['desc'] is None :
					if doc[0].text != None :
						types[nodeName]['desc'] = doc[0].text
						print('[+] ' + nodeName + ' found duplicate definition with Description (Replacing...) ')
					else:
						print ('[-] ' + nodeName + ' found duplicate definition with no Description (Ignoring...)')
				else:
					print ('[-] ' + nodeName + ' found duplicate definition (Keeping first and ignoring rest...) ')
			types[nodeName] = thisDesLabel
	return types

def printElements(filename, types):
	with open(filename,'w') as f:
		today = datetime.date.today()
		f.write('''
/**
 * Generated at {:%d/%b/%Y}
 */
'''.format(today))
		f.write(
'''
export class Description {
    desc : string;
    label : string;
    mandatory : boolean;
    recommended : boolean;
};
''')
		for name in types:
			desc = finalDoc[name]['desc']
			label = finalDoc[name]['label']
			recommended = finalDoc[name]['recommended']
			mandatory = finalDoc[name]['mandatory']
			if label is None:
				label = name
				# print(name + " has no available label")
			if desc is None:
				desc = "Description not available"
			if recommended is None:
				recommended = 'false'
			f.write("export var " + name + "Desc = {\n")
			f.write("\t" + "desc : \"" +   desc.replace('"','\\"') +"\",\n")
			f.write("\t" + "label : \"" + label.replace('"','\\"') +"\",\n")
			f.write("\t" + "mandatory : " + mandatory +",\n")
			f.write("\t" + "recommended : " + recommended +"\n")
			f.write("};\n\n")
		f.close()

def parseEnumValues(node,path,namespace, arr):
	doc = node.xpath(path,namespaces=namespace)
	description = node.xpath("./xs:annotation/xs:appinfo/*[local-name()='label'][1]",namespaces=namespace)
	# print(node.attrib['name'] + '  ' + str(len(description)))
	if len(description)>=1 :
		arr.append( (None , "--{}--".format(description[0].text)) )
	else :
		arr.append( (None , '--{}--'.format(node.attrib['name'])) )

	for enum in doc:
		label = enum.xpath(".//*[local-name()='label']",namespaces=namespace)
		if len(label)==1 :
			arr.append( (enum.attrib['value'] , label[0].text) )
		else :
			arr.append( (enum.attrib['value'] , enum.attrib['value']))

def parseEnums(filename,namespace,types):
	tree = ET.parse(filename)
	root = tree.getroot()
	
	nodes = root.xpath(".//*/xs:enumeration/ancestor::*[@name][1]",namespaces=namespace)
	for node in nodes :
		nodeName = node.attrib['name']
		if nodeName not in types:
			types[nodeName] = []
			parseEnumValues(node,".//xs:enumeration",namespace,types[nodeName])
		else:
			print "Duplicate enum found " + nodeName		
	return types

def printKey(left,right):
	
	rightTmp = right.replace('"','\\"')
	if left is None:
		return u'key : "", value : "{}"'.format(rightTmp)	
	else:	
		leftTmp = Rename.rename(left)
		return u'key : "{}", value : "{}"'.format(leftTmp,rightTmp)

def printEnums(filename, types):

	with codecs.open(filename,'w',"utf-8") as f:
		today = datetime.date.today()
		f.write('''
/**
 * Generated at {:%d/%b/%Y}
 */
'''.format(today))
		f.write(
'''
export class EnumValues {
    key : string;
    value : string;
};
''')
		for name in types:
			f.write("export var " + name + "Enum = [\n")
			for val in types[name][:-1]:
				left,right = val
				f.write("\t{" + printKey(left,right) + "},\n")
			left,right = types[name][-1]
			f.write("\t{" + printKey(left,right) + "}\n")
			f.write("];\n\n")
		f.close()

def parse_files(directory):
	finalDoc = {}
	enums = {}
	namespace = {'xs' : "http://www.w3.org/2001/XMLSchema" , 'ms' : "http://www.meta-share.org/OMTD-SHARE_XMLSchema"}
	for file in os.listdir(directory):
		filename,extension = os.path.splitext(file)
		if extension == ".xsd" :
			parseElements(directory + "/" + file,namespace,finalDoc)
			parseEnums(directory + "/" + file,namespace,enums)
	return (finalDoc,enums)


if __name__ == "__main__" :

	for arg in sys.argv :
		if arg == '-v' :
			verbose = True
		elif arg != sys.argv[0] :
			finalDoc,enums = parse_files(arg)
			printElements('descriptions.ts',finalDoc)
			printEnums('enumerations.ts',enums)
	
	
