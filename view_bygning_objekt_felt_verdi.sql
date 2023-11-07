--pivotfields sql
select OBF_Label FROM OBF_Objekt_Felt ORDER BY OBF_Nr
--main sql
select bygning_navn, eiendom, objekt_navn <<pivot>>
from (
select rtrim(BBY_Navn) as bygning_navn , rtrim(BBO_Navn) as eiendom, rtrim(BBO_Navn) as objekt_navn, OBF_Label, rtrim(OBV_Verdi_Tekst) as vete
    FROM [Abisair].[dbo].[BBY_Bygning]
        inner join BBO_Brannobjekt on BBY_Bygning.BBO_Nr=BBO_Brannobjekt.BBO_Nr
        inner join OBJ_Objekt on BBY_Bygning.bby_nr=OBJ_Objekt.BBY_Nr and BBO_Brannobjekt.BBO_Nr=OBJ_Objekt.BBO_Nr
        inner join OBT_Objekt_Type on dbo.OBJ_Objekt.OBT_Type=dbo.OBT_Objekt_Type.OBT_Type and OBJ_Objekt.BFO_Nr=OBT_Objekt_Type.BFO_Nr
        inner join [dbo].[OBF_Objekt_Felt] on dbo.OBT_Objekt_Type.OBT_Type= dbo.OBF_Objekt_Felt.OBT_Type
        inner join OBV_verdi on OBJ_Objekt.Obj_nr=OBV_Verdi.OBJ_Nr and OBF_Objekt_Felt.OBF_Nr=OBV_Verdi.OBF_Nr
        <<where>>
        ) as bygobj
pivot 
  (max (vete)
  for OBF_Label IN (<<pivot>>)) as pvt
  order by pvt.bygning_navn, pvt.objekt_navn