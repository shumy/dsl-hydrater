grammar HydraterDsl;
@header { package hyd.dsl.antlr; }

root:
  'grammar' (ID '.')* NAME ';'
  'root' ':' expr ';'
  entity*
  EOF
;

entity: NAME ':' expr ';';

expr: '(' expr ')' multiplicity?
  | left=expr oper='&' right=expr
  | left=expr oper='|' right=expr
  | single
  | map
;

single: token=TEXT | ref=NAME ;

map: key=ID assign ;

assign: aExist | aText | aRef | aType ;

  aExist: '-exist->' TEXT ;
  aText: '-text->' TEXT ;
  aRef: '-ref->' NAME ;
  aType: '-type->' type ;

type: 'bool' | 'int' | 'float' | 'date' | 'time' | 'datetime' ;

multiplicity: value=('?' | '+' | '*') ('splitter' '=' splitter=TEXT)?;

// --------------------------------------------------------------
NAME: [A-Z][A-Za-z0-9_]* ;
ID: [a-z][A-Za-z0-9_-]* ;

TEXT: '\'' ( ~['\r\n] )* '\'';

WS: [ \t\r\n\f]+ -> skip ;