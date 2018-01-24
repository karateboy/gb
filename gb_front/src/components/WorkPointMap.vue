<template>
    <div class="row">
        <div class="col-sm-12">
          <div class="form-horizontal">                
                <div class="form-group">
                    <label class="col-sm-2 control-label">種類:</label>
                    <div class="col-lg-10">
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary dim" 
                               v-for="(obj, idx) in typeList" :class="{active: typeIdx==idx}"
                               @click="typeIdx=idx">
                            <input type="checkbox">{{ obj.name }} </label>
                    </div>                    
                </div>
              </div>
            </div>
        </div>
        <div class="col-sm-12">
          <div class="map_container">
            <gmap-map :zoom="12" :center="map_center" ref="map" class="map_canvas">
              <gmap-marker v-for="(dumpSite, index) in dumpSiteList" :key="index" :clickable="true" :title="dumpSite.name" :position="getPosition(dumpSite)" 
                icon="http://icons.iconarchive.com/icons/icons-land/vista-map-markers/48/Map-Marker-Ball-Pink-icon.png"
                @click="toggleInfoWindow('DumpSite', dumpSite, index+1)"
              />
              <gmap-info-window :options="infoOptions" :position="infoWindowPos" :opened="infoWinOpen" @closeclick="infoWinOpen=false">
              {{infoContent}}
              </gmap-info-window>
            </gmap-map>
          </div>
        </div>
    </div>
</template>
<style>
.map_container {
  position: relative;
  width: 100%;
  padding-bottom: 42%;
  /* Ratio 16:9 ( 100%/16*9 = 56.25% ) */
}

.map_container .map_canvas {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  margin: 0;
  padding: 0;
}
</style>
<script>
import axios from "axios";

export default {
  data() {
    return {
      dumpSiteList: [],
      total: 0,
      display: "",
      dropAnimation: google.maps.Animation.DROP,
      currentMidx: 0,
      infoWinOpen: false,
      infoContent: "",
      infoOptions: {
        content: "",
        pixelOffset: {
          width: 0,
          height: -35
        }
      },
      map_center: {
        lng: 120.982024,
        lat: 23.973875
      },
      infoWindowPos: {
        lng: 120.982024,
        lat: 23.973875
      },
      typeIdx: 0,
      typeList: [
        {
          name: "起造人",
          url: "BuildCase"
        },
        {
          name: "長照機構",
          url: "CareHouse"
        }
      ]
    };
  },
  mounted: function() {
    //this.getNearDumpSiteList();
    this.$gmapDefaultResizeBus.$emit("resize");
  },
  methods: {
    updateMap() {
      const bounds = new google.maps.LatLngBounds();
      bounds.extend(this.getPosition(this.buildCase));

      for (let dumpSite of this.dumpSiteList) {
        let pos = this.getPosition(dumpSite);
        bounds.extend(pos);
      }
      this.$refs.map.fitBounds(bounds);
    },
    getNearDumpSiteList() {
      if (this.buildCase.location) {
        let loc = this.buildCase.location;
        axios
          .get(`/Top3DumpSite/${loc[0]}/${loc[1]}`)
          .then(resp => {
            let ret = resp.data;
            this.dumpSiteList.splice(0, this.dumpSiteList.length);
            for (let dumpSite of ret) {
              this.dumpSiteList.push(dumpSite);
            }
            this.updateMap();
          })
          .catch(err => alert(err));
      }
    },
    getPosition(buildCase) {
      return {
        lng: buildCase.location[0],
        lat: buildCase.location[1]
      };
    },
    toggleInfoWindow(markerType, marker, idx) {
      this.infoWindowPos = this.getPosition(marker);
      if (markerType == "BuildCase") {
        this.infoOptions.content =
          `<p><strong>起造人:${marker.builder}</strong><br>` +
          `${marker.siteInfo.addr}<br>` +
          `${marker.siteInfo.usage}<br>` +
          `${marker.siteInfo.floorDesc}<br>` +
          `${marker.siteInfo.area}平方公尺</p>`;
      } else {
        this.infoOptions.content =
          `<p><strong>${marker.name}(第${idx}近)</strong><br>` +
          `${marker.feature}<br>` +
          `${marker.siteType}<br>` +
          `${marker.contact}<br>` +
          `${marker.phone}<br>` +
          `${marker.addr}</p>`;
      }

      //check if its the same marker that was selected if yes toggle
      if (this.currentMidx == idx) {
        this.infoWinOpen = !this.infoWinOpen;
      } else {
        //if different marker set infowindow to open and reset current marker index
        this.infoWinOpen = true;
        this.currentMidx = idx;
      }
    }
  },
  components: {}
};
</script>