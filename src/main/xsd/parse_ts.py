import re
import sys;
import pprint
from parsimonious.grammar import Grammar
from parsimonious.nodes import NodeVisitor
from parsimonious.expressions import (Literal, Regex, Sequence, OneOf,
	Lookahead, Optional, ZeroOrMore, OneOrMore, Not, TokenMatcher,
	expression)
import extract_doc as ed;
import argparse


grammar = Grammar(
	    r"""
	    application  = interface+ types+
	    _ = ~"\s*\n*"
	    classname  = ~"[A-Z0-9<>]*"i
	    extends = "extends" _ classname
	    interface = "export" _ "interface" _ classname _ extends? _ "{" declarations _ "}" _
	    declarations = declaration*
	    types = "export" _ "type" _ varname _ "=" _ enums _ ";" _
	    enums = enum enumtail
	    enumtail = (_ "|" _ enum )*
	    enum = ~'.[A-Z_0-9]*.'i
	    declaration = _ varname _ ":" _ type _ ";" _
	    varname = ~"[A-Z_0-9]+"i
	    type = ~"[A-Z0-9\[\]<>]*"i
	    """)

class EntryParser(NodeVisitor):

	def __init__(self, grammar, text, types, rules):
		self.rules = rules
		self.entry = {}
		self.types = types
		self.ast = grammar.parse(text)
		self.result = ""
		self.visit(self.ast)
		
	def firstLower(self,s):
		return s[:1].lower() + s[1:] if s else ''

	def getCode(self):
		return self.result

	def emit(self,code):
		self.result+=code

	def visit_application(self,n, application):
		interface,types = application;

	def visit_interface(self, n, interface):
		export,_,interfaces,_,classname,_,extends,_,_,declarations,_,_,_ = interface
		ret = "export class " + classname + " " +extends + " {\n"
		if classname in self.rules['omit']  :
			return ""
		elif classname in self.rules['rename']:
			print('Renaming ' + classname + " to " + self.rules['rename'][classname]) 
			classname = self.rules['rename'][classname]

		for dec in declarations :
			ret += '\t' + dec + '\n'
		# ret += "\tdesc:string;\n\tlabel:string;\n"
		ret += "}\n\n"
		self.emit(ret)
		
		clazz = self.firstLower(classname)
		if clazz in self.types:
			if 'desc' in self.types[clazz]:
				self.emit(classname + '.prototype.desc=\"' + self.types[clazz]['desc'] + "\";\n")
			if 'label' in self.types[clazz] and self.types[clazz]['label'] is not None:
				self.emit(classname + '.prototype.label=\"' + self.types[clazz]['label'] + "\";\n\n")
		return ret

	def visit_types(self,n,types):
		export,_,typ,_,varname,_,eq,_,enums,_,q,_ = types
		ret = "export enum " + varname + " {\n"
		for e in enums[:-1] :
			ret += '\t' + e + ',\n'
		ret += '\t' + enums[-1] + '\n'
		ret += "}\n\n"
		self.emit(ret)
		return ret

	def visit_declaration(self,n,declaration):
		_,varname,_,_,_,typ,_,_,_ = declaration;
		if typ in self.rules['omit']:
			typ = self.rules['omit'][typ]
		return varname + " : " + typ + ';'

	def visit_declarations(self,n,declarations):
		return declarations

	def visit_space(self,n,space):
		pass

	def visit__(self,n,_):
		pass

	def visit_enum(self,n,enum):
		st = n.text
		if st :
			return st
		else :
			return None

	def visit_enumtail(self,n,tail) :
		ret = []
		for t in tail:
			m = re.match(r'\s*\| "(\w+)"',t)
			if m and t is not None:
				ret.append(m.group(1))
		return ret

	def visit_enums(self,n,enums):
		enum,tail = enums
		return [enum[1:-1]] + tail

	def visit_varname(self,n,varname) :
		return n.text

	def visit_type(self,n,typ):
		return n.text

	def visit_extends(self,n,extends):
		extend,_,clazz = extends
		return extend + " " + clazz

	def visit_classname(self,n,clazz):
		return n.text

	def generic_visit(self, n, vc):
		return n.text



def process_interface(filename,types) :
	prog = re.compile('(export\s+interface\s+([\w<>]+)\s+{)')
	rules = {}
	rules['omit'] = {}
	rules['rename'] = {}
	rules['omit']['XMLGregorianCalendar'] = 'Date'
	rules['rename']['Component'] = "OMTDComponent"
	rules['rename']['Corpus'] = "OMTDCorpus"
	rules['rename']['LanguageDescription'] = "OMTDLanguageDescription"
	rules['rename']['Model'] = "OMTDModel"
	rules['rename']['LexicalConceptualResource'] = "OMTDLexicalConceptualResource"

	with open(filename) as file:
		ent = EntryParser(grammar, file.read(), types, rules)
		file.close()
		return ent.getCode()


if __name__ == '__main__' :
	parser = argparse.ArgumentParser(description="Parses the xsd files and injects code into the typescript generated file")
	parser.add_argument('-d', '--directory', type=str, required=True, help = "the directory containt the xsd")
	parser.add_argument('-n', '--name', type=str, help = "the name of the generated typescript", default = "sample.d.ts")
	parser.add_argument('-o', '--output', type=str, help = "the name of the generated typescript", default = "injsample.d.ts")
	args = parser.parse_args()

	types = ed.parse_files(args.directory)
	code = process_interface(args.name,types)
	with open(args.output,'w') as w :
		w.write(code)
		w.close()
	