SELECT TOP (1000) [BKB_Nr]
      ,[BFO_Nr]
      ,[BBO_Nr]
      ,[BVV_Type]
      ,[BVV_Kode]
      ,[BVV_Tekst]
      ,[BVV_Verdi]
      ,[BVV_Gruppe]
      ,[BVV_Ajourniva]
      ,[BVV_Inaktiv]
  FROM [Abisair].[dbo].[BVV_Valgbare_verdier]
  SELECT TOP (1000) [OBF_Nr]
      ,[OBF_Label]
      ,[OBF_Type_Felt]
      ,[BVV_Type]
      ,[OBF_Sekvens]
      ,[OBF_Blankes]
      ,[OBF_Vise_i_Grid]
      ,[OBF_Vise_i_PDF]
      ,[BFO_Nr]
      ,[OBT_Type]
  FROM [Abisair].[dbo].[OBF_Objekt_Felt]
  SELECT TOP (1000) [OBV_Nr]
      ,[OBJ_Nr]
      ,[OBF_Nr]
      ,[OBV_Verdi_Tall]
      ,[OBV_Verdi_Tekst]
      ,[OBV_Verdi_Dato]
      ,[BKL_Nr]
  FROM [Abisair].[dbo].[OBV_Verdi]

  