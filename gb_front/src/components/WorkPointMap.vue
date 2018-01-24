<template>
    <div class="row">
        <div class="col-sm-12">
          <div class="form-horizontal">                
                <div class="form-group">
                    <label class="col-sm-2 control-label">種類:</label>
                    <div class="col-sm-10">
                    <div data-toggle="buttons-checkbox" class="btn-group">
                        <button class="btn btn-primary" type="button" v-for="obj in typeList" :key="obj._id" :class="{active: selectedTypeList.indexOf(obj.typeID)!=-1}">
                          <input type="checkbox" :value="obj.typeID" v-model="selectedTypeList"> {{ obj.name }}</button>                            
                    </div>                    
                </div>
              </div>
            </div>
        </div>
        <div class="col-sm-12">
          <div class="map_container">
            <gmap-map :zoom="15" :center="default_center" ref="map" class="map_canvas" @center_changed="updateCenter" @bounds_changed="updateBound($event)">
              <gmap-marker v-for="(workPoint, index) in workPointList" :key="index" :clickable="true" :title="workPoint.summary.title" :position="getPosition(workPoint)" 
                :icon="getIcon(workPoint)"
                @click="toggleInfoWindow(workPoint, index)"
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
  padding-bottom: 50%;
  /*height: 400px;*/
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
      workPointList: [],
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
      default_center: {
        lng: 120.982024,
        lat: 23.973875
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
      typeList: [],
      selectedTypeList: ["BuildCase"],
      mapBounds: new google.maps.LatLngBounds()
    };
  },
  watch: {
    selectedTypeList() {
      this.getAreWorkPointList();
    }
  },
  mounted: function() {
    this.getWorkPointType();
    this.getCurrentPos();
    this.$gmapDefaultResizeBus.$emit("resize");
  },
  methods: {
    getCurrentPos() {
      if (navigator.geolocation) {
        let geo = navigator.geolocation;
        let option = {
          enableAcuracy: false,
          maximumAge: 0,
          timeout: 600000
        };

        geo.getCurrentPosition(
          position => {
            this.default_center = {
              lng: position.coords.longitude,
              lat: position.coords.latitude
            };
          },
          null,
          option
        );
      }
    },
    updateCenter(newCenter) {
      this.map_center = {
        lat: newCenter.lat(),
        lng: newCenter.lng()
      };
    },
    updateBound(evt) {
      this.mapBounds = evt;
      this.getAreWorkPointList();
    },
    getWorkPointType() {
      axios
        .get("/WorkPointType")
        .then(resp => {
          const ret = resp.data;
          this.typeList.splice(0, this.typeList.length);
          for (let type of ret) {
            this.typeList.push(type);
          }
        })
        .catch(err => alert(err));
    },
    updateMap() {
      const bounds = new google.maps.LatLngBounds();

      bounds.extend(this.map_center);

      for (let workPoint of this.workPointList) {
        let pos = this.getPosition(workPoint);
        bounds.extend(pos);
      }
      this.$refs.map.fitBounds(bounds);
    },
    getAreWorkPointList() {
      let typeIDs = this.selectedTypeList.join();
      let bl = this.mapBounds.getNorthEast().toJSON();
      let ur = this.mapBounds.getSouthWest().toJSON();
      axios
        .get(
          `/WorkPoint/${typeIDs}/${JSON.stringify(bl)}/${JSON.stringify(ur)}`
        )
        .then(resp => {
          let ret = resp.data;
          this.workPointList.splice(0, this.workPointList.length);
          for (let workPoint of ret) {
            this.workPointList.push(workPoint);
          }
          //this.updateMap();
        })
        .catch(err => alert(err));
    },
    getPosition(workPoint) {
      return {
        lng: workPoint.location[0],
        lat: workPoint.location[1]
      };
    },
    toggleInfoWindow(wp, idx) {
      this.infoWindowPos = this.getPosition(wp);

      this.infoOptions.content =
        `<p><strong>${wp.summary.title}</strong><br>` +
        `${wp.summary.content}</p>`;

      //check if its the same marker that was selected if yes toggle
      if (this.currentMidx == idx) {
        this.infoWinOpen = !this.infoWinOpen;
      } else {
        //if different marker set infowindow to open and reset current marker index
        this.infoWinOpen = true;
        this.currentMidx = idx;
      }
    },
    getIcon(wp) {
      switch (wp._id.wpType) {
        case 1:
          return "http://icons.iconarchive.com/icons/icons-land/transport/48/Excavator-icon.png";
        case 2:
          return "http://icons.iconarchive.com/icons/dapino/medical/48/nurse-icon.png";
        case 3:
          return "http://icons.iconarchive.com/icons/creative-freedom/shimmer/48/Recycle-icon.png";
        case 4:
          return "http://icons.iconarchive.com/icons/aha-soft/large-home/48/Hangar-icon.png";
        case 5:
          return "http://icons.iconarchive.com/icons/icons-land/gis-gps-map/48/GasStation-icon.png";
        default:
          return "http://icons.iconarchive.com/icons/icons-land/vista-map-markers/48/Map-Marker-Ball-Pink-icon.png";
      }
    }
  },
  components: {}
};
</script>