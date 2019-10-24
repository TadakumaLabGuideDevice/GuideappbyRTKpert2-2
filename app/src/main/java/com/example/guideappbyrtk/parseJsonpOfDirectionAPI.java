package com.example.guideappbyrtk;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class parseJsonpOfDirectionAPI {
    //Googlemap関連------------------------------------------------------------------------

    public static String posinfo = "";
    public static String info_A = "";
    public static String info_B = "";

    public List<List<HashMap<String,String>>> parse(JSONObject jObject){
        String temp = "";

        List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String,String>>>() ;
        JSONArray jsonRoutes;
        JSONArray jsonLegs;
        JSONArray jsonSteps;

        try {

            jsonRoutes = jObject.getJSONArray("routes");

            for(int i=0;i<jsonRoutes.length();i++){
                jsonLegs = ( (JSONObject)jsonRoutes.get(i)).getJSONArray("legs");

                //スタート地点・住所
                String s_address = (String)((JSONObject)(JSONObject)jsonLegs.get(i)).getString("start_address");

                info_A = s_address;

                //到着地点・住所
                String e_address = (String)((JSONObject)(JSONObject)jsonLegs.get(i)).getString("end_address");

                info_B = e_address;

                String distance_txt = (String)((JSONObject)((JSONObject)jsonLegs.get(i)).get("distance")).getString("text");

                temp += distance_txt + "<br><br>";

                String distance_val = (String)((JSONObject)((JSONObject)jsonLegs.get(i)).get("distance")).getString("value");

                temp += distance_val + "<br><br>";

                List path = new ArrayList<HashMap<String, String>>();


                for(int j=0;j<jsonLegs.length();j++){
                    jsonSteps = ( (JSONObject)jsonLegs.get(j)).getJSONArray("steps");


                    for(int k=0;k<jsonSteps.length();k++){
                        String polyline = "";
                        polyline = (String)((JSONObject)((JSONObject)jsonSteps.get(k)).get("polyline")).get("points");


                        String instructions = (String)((JSONObject)(JSONObject)jsonSteps.get(k)).getString("html_instructions");
                        String duration_value = (String)((JSONObject)((JSONObject)jsonSteps.get(k)).get("duration")).getString("value");
                        String duration_txt = (String)((JSONObject)((JSONObject)jsonSteps.get(k)).get("duration")).getString("text");

                        temp += instructions + "/" + duration_value + " m /" + duration_txt + "<br><br>";


                        List<LatLng> list = decodePoly(polyline);


                        for(int l=0;l<list.size();l++){
                            HashMap<String, String> hm = new HashMap<String, String>();
                            hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
                            hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
                            path.add(hm);
                        }
                    }
                    //ルート座標
                    routes.add(path);
                }

                //ルート情報
                posinfo = temp;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
        }

        return routes;
    }

    //座標データをデコード
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
