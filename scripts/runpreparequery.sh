fSource=$1
#fSource="Die"
fEdge="4es${fSource:0:1}"
tSource=$2
#tSource="deutschen"
tEdge="3es${tSource:0:1}"
sSource=$3
#sSource="Bauern"
sEdge="2es${sSource:0:1}"
oSource=$4
#oSource="haben"
oEdge="1es${oSource:0:1}"
prefix="${5}%"
#prefix="d%"

query="select ${fEdge}.source, ${tEdge}.source, ${sEdge}.source, ${oEdge}.source, ${fEdge}.target, (IFNULL(${fEdge}.score, 0) + IFNULL(${tEdge}.score, 0) + IFNULL(${sEdge}.score, 0) + IFNULL(${oEdge}.score, 0)) as count from ${fEdge} right outer join ${tEdge} on ${tEdge}.target=${fEdge}.target right outer join ${sEdge} on ${sEdge}.target=${fEdge}.target right outer join ${oEdge} on ${oEdge}.target like ${fEdge}.target where ${fEdge}.source like \"${fSource}\" AND ${fEdge}.target like \"${prefix}\" and ${tEdge}.source like \"${tSource}\" AND ${tEdge}.target like \"${prefix}\" and ${sEdge}.source like \"${sSource}\" AND ${sEdge}.target like \"${prefix}\" and ${oEdge}.source like \"${oSource}\" AND ${oEdge}.target like \"${prefix}\" order by count desc limit 5;"

mysql -u importer typology --local-infile=1 -e "${query}"
