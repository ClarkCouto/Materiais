create or replace PROCEDURE PR_RETIRAR_ACENTOS IS  
BEGIN

  FOR t IN (/* BUSCAR O NOME DAS TABELAS E DAS COLUNAS A SEREM ALTERADAS */
            SELECT TABELAS.TABLE_NAME AS TABELA, COLUNAS.COLUMN_NAME AS COLUNA
                FROM USER_TABLES TABELAS, USER_TAB_COLUMNS COLUNAS
                WHERE TABELAS.TABLE_NAME = COLUNAS.TABLE_NAME
                  AND COLUNAS.DATA_TYPE = 'VARCHAR2'
                  /* EXCLUI TODAS AS TABELAS QUE SÃO DE INDEX, POIS SÃO PK */
                  AND COLUNAS.COLUMN_NAME NOT LIKE 'cd%'
                  AND TABELAS.TABLE_NAME IN (/* PERCORRE TODAS AS TABELAS QUE SÃO NOSSAS */
                                            SELECT table_name
                                                FROM user_tables where TABLE_NAME LIKE 'SXIC%'
                                                UNION              
                                                SELECT table_name
                                                FROM user_tables where TABLE_NAME LIKE 'SXGC%'
                                                UNION
                                                SELECT table_name
                                                FROM user_tables where TABLE_NAME LIKE 'SXCG%'
                                                UNION
                                                SELECT table_name
                                                FROM user_tables where TABLE_NAME IN ('FORMA_PGTO',
                                                                                      'FORNECEDOR', 
                                                                                      'GRUPO_FOR_CLI', 
                                                                                      'HOSPITAL',
                                                                                      'TIPO_FORNECEDOR', 
                                                                                      'USUARIO',
                                                                                      'USUARIO_ASSOCIACAO'))) 
  LOOP
    --MOSTRA AS COLUNAS QUE ESTÃO SENDO ATUALIZADAS
    Dbms_Output.Put_Line(Systimestamp || 'tabela ==> ' || t.TABELA || ' coluna ==> ' || t.COLUNA );
    EXECUTE IMMEDIATE 
    'UPDATE '||t.TABELA||' SET '||t.COLUNA||' = REMOVE_ACENTOS('||t.COLUNA||')';
  END LOOP;
END;