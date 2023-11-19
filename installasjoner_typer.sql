SELECT  distinct trim([OBT_Type]) as installasjon_kode
      ,trim([OBT_Tekst]) as installasjon
       FROM [Abisair].[dbo].[OBT_Objekt_Type] 
WHERE 'INST_'+trim([OBT_Type]) IN (
  SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_CATALOG='Abisair' AND TABLE_NAME LIKE 'INST_%')