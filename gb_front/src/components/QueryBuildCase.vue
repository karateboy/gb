<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group"><label class="col-lg-1 control-label">有座標:</label>
                <div class="col-lg-4"><input type="checkbox"
                                             class="form-control"
                                             v-model="queryParam.hasLocation">
                </div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">起造人案名:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder="起造人"
                                             v-model="queryParam.name"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">建築師:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder=""
                                             v-model="queryParam.architect"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">縣市:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder="新北市"
                                             v-model="queryParam.county"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">地址:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder="地址"
                                             v-model="queryParam.addr"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">樓板面積(大於):</label>
                <div class="col-lg-4"><input type="number" class="form-control"
                                             v-model="queryParam.areaGT"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">樓板面積(小於):</label>
                <div class="col-lg-4"><input type="number" class="form-control"
                                             v-model="queryParam.areaLT"></div>
            </div>
            <div class="form-group">
                <div class="col-lg-1 col-lg-offset-1">
                    <button class='btn btn-primary' @click='query'>查詢</button>
                </div>
                <div class="col-lg-1 col-lg-offset-1">
                    <button class='btn btn-primary' @click='queryMap'>顯示地圖</button>
                </div>
                <!--                <div class="col-lg-1 col-lg-offset-1">
                                    <button class="btn btn-info" @click='exportExcel'>Excel</button>
                                </div>-->
            </div>
        </div>
        <div v-if='display'>
            <build-case-list url="/QueryBuildCase" :param="queryParam"></build-case-list>
        </div>
        <div v-if='showMap'>
            <build-case-map url="/QuerybuildCase" :param="queryParam"></build-case-map>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
    import axios from 'axios'
    import moment from 'moment'
    import BuildCaseList from "./BuildCaseList.vue"
    import BuildCaseMap from "./BuildCaseMap.vue"

    export default {
        data() {
            return {
                display: false,
                showMap: false,
                queryParam: {
                    hasLocation: true
                }
            }
        },
        methods: {
            prepareParam() {
            },
            query() {
                this.prepareParam()
                if (!this.display)
                    this.display = true

                this.queryParam = Object.assign({}, this.queryParam)
            },
            queryMap(){
                this.prepareParam()
                if (!this.showMap)
                    this.showMap = true

                this.queryParam = Object.assign({}, this.queryParam)
            }
        },
        components: {
            BuildCaseList,
            BuildCaseMap
        }
    }
</script>
