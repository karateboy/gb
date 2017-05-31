<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group"><label class="col-lg-1 control-label">公立:</label>
                <div class="col-lg-4"><input type="checkbox"
                                             class="form-control"
                                             v-model="queryParam.isPublic">
                </div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">機構名稱:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder="養護中心"
                                             v-model="queryParam.name"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">負責人:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder=""
                                             v-model="queryParam.principal"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">區域:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder="區域"
                                             v-model="queryParam.district"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">地址:</label>
                <div class="col-lg-4"><input type="text" class="form-control" placeholder="地址"
                                             v-model="queryParam.addr"></div>
            </div>
            <div class="form-group">
                <div class="col-lg-offset-1">
                    <button class='btn btn-primary' @click='query'>查詢</button>
                </div>
            </div>
        </div>
        <div v-if='display'>
            <div v-if='orderList.length != 0'>
                <order-list :order-list="careHouseList"></order-list>
            </div>
            <div v-else class="alert alert-info">沒有符合的機構</div>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
    import axios from 'axios'
    import moment from 'moment'
    import CareHouseList from "./CareHouseList.vue"

    export default{
        data(){
            return {
                display: false,
                careHouseList: [],
                queryParam: {}
            }
        },
        computed: {},
        methods: {
            prepareParam(){

                if (this.queryParam.brand == "")
                    this.queryParam.brand = null

                if (this.queryParam.name == "")
                    this.queryParam.name = null

                if (this.queryParam.principal == "")
                    this.queryParam.principal = null

                if (this.queryParam.district == '')
                    this.queryParam.district = null

                if (this.queryParam.district == '')
                    this.queryParam.district = null

            },
            query(){
                this.prepareParam()
                axios.post('/QueryCareHouse', this.queryParam).then((resp) => {
                    const ret = resp.data
                    this.careHouseList.splice(0, this.careHouseList.length)
                    for (let careHouse of ret) {
                        this.careHouseList.push(careHouse)
                    }
                    this.display = true
                }).catch((err) => {
                    alert(err)
                })
            }
        },
        components: {
            CareHouseList
        }
    }
</script>
