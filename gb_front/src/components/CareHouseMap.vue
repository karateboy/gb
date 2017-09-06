<template>
    <div class="row">
        <div class="col-lg-12">
            <div class="ibox ">
                <div class="ibox-content">
                    <div class="map_container">
                        <gmap-map :center="{lat:1.38, lng:103.8}" :zoom="12" class="map_canvas">
                            <gmap-marker :position="{lat:1.38, lng:103.8}">
                            </gmap-marker>
                            <gmap-info-window :position="{lat:1.38, lng:103.8}">
                                Hello world!
                            </gmap-info-window>
                        </gmap-map>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<style>
    .map_container{
        position: relative;
        width: 100%;
        padding-bottom: 42%; /* Ratio 16:9 ( 100%/16*9 = 56.25% ) */
    }
    .map_container .map_canvas{
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
        margin: 0;
        padding: 0;
    }

    .realtimeChart_container{
        position: relative;
        width: 120%;
    }
</style>
<script>
    import axios from 'axios'

    export default {
        props: {
            url: {
                type: String,
                required: true
            },
            param: {
                type: Object
            }
        },
        data() {
            return {
                careHouseList: [],
                total: 0,
                display: "",
            }
        },
        mounted: function () {
            this.$gmapDefaultResizeBus.$emit('resize')
            this.fetchCareHouse()
        },
        watch: {
            url: function (newUrl) {
                this.fetchCareHouse(this.skip, this.limit)
            },
            param: function (newParam) {
                this.fetchCareHouse(this.skip, this.limit)
            }
        },

        methods: {
            processResp(resp) {
                const ret = resp.data
                this.careHouseList.splice(0, this.careHouseList.length)

                for (let careHouse of ret) {
                    this.careHouseList.push(careHouse)
                }
                console.log("#=" + this.careHouseList.length)
            },
            fetchCareHouse() {
                let request_url = `${this.url}`

                if (this.param) {
                    axios.post(request_url, this.param).then(this.processResp).catch((err) => {
                        alert(err)
                    })
                } else {
                    axios.get(request_url).then(this.processResp).catch((err) => {
                        alert(err)
                    })
                }
            },
            initMap() {
            }
        },
        components: {}
    }
</script>
