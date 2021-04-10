grammar HydraterDsl;
@header { package hyd.dsl.antlr; }

root:
  'grammar' identity ';'
  entity*
  EOF
;

entity:
  checker*
  NAME ':' expr ';'
;

checker: key=ID '@' '[' (identity (',' identity)*) ']' ;

expr: '(' expr ')' multiplicity?
  | left=expr oper='|'? right=expr
  | token
  | map
;

token: TEXT | NAME ;

map: key=ID assign ;

assign: aExist | aRef | aType ;

  aExist: '-exist->' TEXT ;
  aRef: '-ref->' NAME multiplicity? ;
  aType: '-type->' type multiplicity? ;

type: value=('bool' | 'text' | 'int' | 'float' | 'date' | 'time' | 'datetime')  ;

multiplicity: value=('?' | '+' | '*') splitter=TEXT?;

identity: (ID '.')* NAME ;

// --------------------------------------------------------------
NAME: [A-Z][A-Za-z0-9_]* ;
ID: [a-z][A-Za-z0-9_-]* ;
TEXT: '\'' ( ~['\r\n] )* '\'';

WS: [ \t\r\n\f]+ -> skip ;