import xml.dom.minidom as dom
import sys
import xml.etree.ElementTree as ET

if __name__ == "__main__" :
	for arg in sys.argv :
		if arg != sys.argv[0] :
			tree = ET.parse(arg)
			root = tree.getroot()
			xmlstr = dom.parseString(ET.tostring(root)).toprettyxml(newl='',indent='')
			with open('form_' + arg, 'w') as f :
				f.write(xmlstr.encode('utf-8'))
				f.close()