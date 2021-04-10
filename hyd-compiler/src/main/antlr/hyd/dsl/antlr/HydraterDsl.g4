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
  | map
  | end // should I put multiplicity here?
;

map: key=ID '=' assign multiplicity? ;

assign: ('&' ref=NAME) | or | end ;

// --------------------------------------------------------------
or: '(' (end ('|' end)*) ')' ;

end: TEXT | NAME | type ;

type: 'bool' | 'text' | 'int' | 'float' | 'date' | 'time' | 'datetime' | 'embedded' | 'ref' ;

multiplicity: value=('?' | '+' | '*') splitter=TEXT?;

identity: (ID '.')* NAME ;

// --------------------------------------------------------------
NAME: [A-Z][A-Za-z0-9_]* ;
ID: [a-z][A-Za-z0-9_-]* ;
TEXT: '\'' ( ~['\r\n] )* '\'';

WS: [ \t\r\n\f]+ -> skip ;