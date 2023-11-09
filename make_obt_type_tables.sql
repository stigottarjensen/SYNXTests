--pivotfields sql
select distinct trim(OBF_Label) as pivotfields FROM OBF_Objekt_Felt
--main sql
select bygning_navn, eiendom, objekt_type, objekt_tekst, <<pivotfields>>
from (
select bygning_navn , rtrim(BBO_Navn) as eiendom, rtrim(OBT_Objekt_Type.OBT_Type) as objekt_type, rtrim(OBT_Tekst) as objekt_tekst, OBF_Label, rtrim(OBV_Verdi_Tekst) as vete
    FROM [Abisair].[dbo].[view_bygning]
        inner join BBO_Brannobjekt on view_bygning.object_nr=BBO_Brannobjekt.BBO_Nr
        inner join OBJ_Objekt on view_bygning.bygning_nr=OBJ_Objekt.BBY_Nr and BBO_Brannobjekt.BBO_Nr=OBJ_Objekt.BBO_Nr
        inner join OBT_Objekt_Type on dbo.OBJ_Objekt.OBT_Type=dbo.OBT_Objekt_Type.OBT_Type and OBJ_Objekt.BFO_Nr=OBT_Objekt_Type.BFO_Nr
        inner join [dbo].[OBF_Objekt_Felt] on dbo.OBT_Objekt_Type.OBT_Type= dbo.OBF_Objekt_Felt.OBT_Type
        inner join OBV_verdi on OBJ_Objekt.Obj_nr=OBV_Verdi.OBJ_Nr and OBF_Objekt_Felt.OBF_Nr=OBV_Verdi.OBF_Nr
        <<where>>
        ) as bygobj pivot 
  (max (vete)
  for OBF_Label IN (<<pivotfields>>)) as pvt
  order by pvt.objekt_type