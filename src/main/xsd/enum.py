import xml.etree.ElementTree as ET
import xml.dom.minidom as dom
import sys
import os
import pprint
import re
import argparse

MAX_ENUM = 300
verbose = False


namespace = {'xs' : "http://www.w3.org/2001/XMLSchema" , 'ms' : "http://www.meta-share.org/OMTD-SHARE_XMLSchema"}

class Rename:

	@staticmethod
	def rename(name):
		if name is '':
			name = 'BLANK'
		elif name is 'null' :
			name = 'null'
		new_name = name
		match = re.match('^[^0-9][a-zA-Z_0-9]+$',name)
		if match is None:
			new_name = re.sub(r'\s',"",name)
			new_name = re.sub(r'/',"_",name)
			new_name = re.sub(r'[^\w\d_]','_', new_name)
			if re.match('\d.*',new_name) is not None :
				new_name = 'V' + new_name
		else:
			new_name = re.sub(r'([a-z])([A-Z])',r"\1_\2",name)
		return new_name.upper()


def create_appInfo(node) :
	annotation = node.findall('./xs:annotation',namespace) 
	if not annotation:
		#print 'Creating xs:annotation node under ' + node.tag
		annotation = ET.Element('xs:annotation')
		node.insert(0,annotation)
		appInfo = ET.SubElement(annotation,'xs:appinfo')
	else:
		#print 'Using existing xs:annotation node'
		annotation = annotation[0]
		appInfo = annotation.findall('./xs:appinfo',namespace)
		if len(appInfo) == 0 :
			#print '\tCreating new appInfo node'
			appInfo = ET.SubElement(annotation,'xs:appinfo')
		else:
			appInfo = appInfo[0]
			#print 'Using ' + appInfo[0].tag

	return appInfo

def rename_enum(n, appInfo,node) :
	enums = n.findall('./xs:restriction/xs:enumeration',namespace)
	if len(enums) > 0 :
		cl = ET.SubElement(appInfo,'jaxb:typesafeEnumClass')
		if len(enums) > MAX_ENUM :
			cl.set('map','false')
	if 'name' in node.attrib:
		cl.set('name', node.attrib['name'] + 'Enum')
		if verbose :
			print 'Naming the enum ' + node.attrib['name']
	if len(enums) > 0 : 
		for enum in enums :
			if 'value' in enum.attrib :
				if len(enum.attrib['value']) == 0 :
					rename = 'BLANK'
					if verbose :
						print '\tReplacing empty string \tto\t ' + rename.upper()
					clRenaming = ET.Element('jaxb:typesafeEnumMember')
					clRenaming.set('value',enum.attrib['value'])
					clRenaming.set('name',rename.upper())
					cl.insert(0,clRenaming)
				else:
					rename = enum.attrib['value']
					if re.match('^[^0-9][a-zA-Z_+\-=/0-9]+$',rename) is None:
						rename = re.sub(r'\s',"",enum.attrib['value'])
						rename = re.sub(r'[^\w\d_]','_', rename)			
						if re.match('\d.*',rename) is not None :
							rename = 'V' + rename	
						if rename != enum.attrib['value'] :
							clRenaming = ET.Element('jaxb:typesafeEnumMember')
							clRenaming.set('value',enum.attrib['value'])
							clRenaming.set('name',rename.upper())
							cl.insert(0,clRenaming)
						if verbose :
							print '\tRenaming <<<' + enum.attrib['value'] + '>>> \tto\t ' + rename.upper()
				#else:
					#print '\tParsing <<<'+enum.attrib['value'] + '>>>'

def simplify_choices(node):
	appInfo = create_appInfo(node)
	ET.SubElement(appInfo,"simplify:as-element-property")

def rename_element(node,package):
	if 'name' in node.attrib:
		match = re.match(r'(\w+)Type$',node.attrib['name'])
		if match is not None:
			elemName = match.group(1)
			print(elemName)
			appInfo = create_appInfo(node)
			elemName = elemName[0].capitalize() + elemName[1:]
			name = {'name' : elemName}
			ET.SubElement(appInfo,'jaxb:class',attrib=name)

def modify(filename,args) :
	ET.register_namespace("ms","http://www.meta-share.org/OMTD-SHARE_XMLSchema")
	ET.register_namespace("xs","http://www.w3.org/2001/XMLSchema")
	ET.register_namespace("","http://www.meta-share.org/OMTD-SHARE_XMLSchema")
	tree = ET.parse(filename)
	root = tree.getroot()

	root.set('xmlns:ms',"http://www.meta-share.org/OMTD-SHARE_XMLSchema")
	root.set('xmlns:jaxb',"http://java.sun.com/xml/ns/jaxb")
	root.set('jaxb:version',"1.0")
	root.set('xmlns:xjc',"http://java.sun.com/xml/ns/jaxb/xjc")
	root.set('xmlns:simplify',"http://jaxb2-commons.dev.java.net/basic/simplify")
	root.set('jaxb:extensionBindingPrefixes',"xjc simplify")

	nodes = root.findall(".//xs:simpleType/xs:restriction/xs:enumeration/../../..",namespace)
	for node in nodes :
		for n in node.findall("./xs:simpleType",namespace) :
			appInfo = create_appInfo(n)
			rename_enum(n,appInfo,node)

	nodes = root.findall(".//xs:choice",namespace)
	for node in nodes :
		simplify_choices(node)
			
	nodes = root.findall(".//xs:complexType",namespace)
	for node in nodes :
		rename_element(node,args.package)

	# nodes = root.findall(".//xs:element",namespace)
	# for node in nodes :
	# 	rename_element(node,args.package)

	return root

def write_tree(root,filename):
	xmlstr = dom.parseString(ET.tostring(root)).toprettyxml(newl='',indent='')
	with open(filename, 'w') as f :
		f.write(xmlstr.encode('utf-8'))
		f.close()


def process_dir(dir,args):
	print(dir)
	for root, dirs, files in os.walk(dir):
		for file in files:
			print(file)
			if file.endswith(".xsd"):
				modfile = os.path.basename(root + file)
				original = os.path.abspath(root + file)
				if os.path.isfile(modfile) :
					statsM = os.stat(modfile)
					statsO = os.stat(original)		
					if statsM.st_mtime < statsO.st_mtime :
						print '[+] Original file modified creating ' + modfile
						xml = modify(original,args)
						write_tree(xml,modfile)
					elif args.force:
						print '[+] Force create ' + modfile
						xml = modify(original,args)
						write_tree(xml,modfile)
					else:
						print '[ ] Unchanged file ' + modfile
				else:
					print '[+] Creating file ' + modfile

					xml = modify(original,args)
					write_tree(xml,modfile)

if __name__ == "__main__" :	
	parser = argparse.ArgumentParser(description="Parses the xsd files and creates new ones with jaxb rules")
	parser.add_argument('-d', '--directory', type=str, required=True, help = "the directory containing the xsd")
	parser.add_argument('-p', '--package', type=str, default='eu.openminted.registry.domain',help = "The package folder of the generated classes")
	parser.add_argument('-v', '--verbose', type=bool, help = "Toggle verbosity", default = False)
	parser.add_argument('-f', '--force', type=bool, help = "Enforce changes", default = False)
	args = parser.parse_args()
	print("Parsing files in directory {}".format(args.directory))
	verbose = args.verbose
	process_dir(args.directory,args)
