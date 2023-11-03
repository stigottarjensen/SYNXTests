SELECT top(2000) [BBY_Nr]
      ,[BBO_Nr] as bygning_nr
      ,trim([BBY_Navn]) as [bygning_navn]
      ,trim([BBY_Gate]) as bygning_adresse
      ,trim([BBY_Kommune]) as bygning_kommune
      ,trim([BBY_Gnr_Bnr_Snr]) as bygning_gardBrSNr
      ,trim([BBY_Bygningstype]) as [bygning_bygningstype]
      ,[BBY_Byggear] as bygning_byggeaar
      ,[BBY_Ferdigattest] as bygning_ferdigattest
      ,trim([BBY_Pabygg]) as bygning_pabygg
      ,[BBY_Rehabilitert]
      ,trim([BBY_Bygningskonstr]) as bygning_konstruksjon
      ,[BBY_Grunnflate] as [bygning_grunnflate]
      ,[BBY_Bruttoareal] as bygning_bruttoareal
      ,[BBY_Etasjer] as bygning_etasjer
      ,trim([BBY_Kjeller_Loft_Under]) as bygning_kjellerLoftUnder
      ,[BBY_Sarskilt_brannobjekt]
      ,[BBY_Ref_Nr]
      ,trim([BBY_Verneverdig]) as bygning_verneverdig
      ,[BBY_Rehabilitert_ar]
  FROM [Abisair].[dbo].[BBY_Bygning] 
