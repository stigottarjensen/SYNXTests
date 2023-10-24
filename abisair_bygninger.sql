SELECT TOP (3) [BBY_Nr]
      ,[BBO_Nr]
      ,trim([BBY_Navn]) as Bygningsnavn
      ,trim([BBY_Gate]) as Bygningsadresse
      ,trim([BBY_Kommune]) as Kommune
      ,trim([BBY_Gnr_Bnr_Snr]) as GardBrSNr
      ,trim([BBY_Bygningstype]) as Bygningstype
      ,[BBY_Byggear]
      ,[BBY_Ferdigattest]
      ,trim([BBY_Pabygg]) as Pabygg
      ,[BBY_Rehabilitert]
      ,trim([BBY_Bygningskonstr]) as Konstruksjon
      ,[BBY_Grunnflate]
      ,[BBY_Bruttoareal]
      ,[BBY_Etasjer]
      ,trim([BBY_Kjeller_Loft_Under]) as KjellerLoftUnder
      ,[BBY_Sarskilt_brannobjekt]
      ,[BBY_Ref_Nr]
      ,trim([BBY_Verneverdig]) as Verneverdig
      ,[BBY_Rehabilitert_ar]
  FROM [Abisair].[dbo].[BBY_Bygning] 
for JSON path