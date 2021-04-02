grammar HydraterDsl;
@header { package hyd.dsl.antlr; }

root:
  'grammar' identity ';'
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
  aRef: '-ref->' NAME multiplicity? ;
  aType: '-type->' type multiplicity? ;

type: value=('bool' | 'text' | 'int' | 'float' | 'date' | 'time' | 'datetime') ('@' checker=identity)? ;

multiplicity: value=('?' | '+' | '*') splitter=TEXT?;

identity: (ID '.')* NAME ;

// --------------------------------------------------------------
NAME: [A-Z][A-Za-z0-9_]* ;
ID: [a-z][A-Za-z0-9_-]* ;
TEXT: '\'' ( ~['\r\n] )* '\'';

WS: [ \t\r\n\f]+ -> skip ;