<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-lg-1 control-label">縣市:</label>
                <div class="col-lg-4"><input type="text" class="form-control" v-model="buildCase.county"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">起造人案名:</label>
                <div class="col-lg-4"><input type="text" class="form-control" v-model="buildCase.name"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">建築師:</label>
                <div class="col-lg-4"><input type="text" class="form-control" v-model="buildCase.architect"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">地址:</label>
                <div class="col-lg-4"><input type="text" class="form-control" v-model="buildCase.addr"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">送件日:</label>
                <div class="col-lg-4"><input type="date" class="form-control" v-model="myDate"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">承造單位(含可能得標者):</label>
                <div class="col-lg-4"><input type="text" class="form-control" v-model="buildCase.builder"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">電話:</label>
                <div class="col-lg-4"><input type="tel" class="form-control" v-model="buildCase.phone"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">簽約:</label>
                <div class="col-lg-4"><input type="checkbox" class="form-control" v-model="buildCase.contracted">
                </div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">最後拜訪日:</label>
                <div class="col-lg-4"><input type="date" class="form-control" v-model="lastVisit">
                </div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">業務:</label>
                <div class="col-lg-4"><input type="text" class="form-control" v-model="buildCase.sales"></div>
            </div>
            
            <div class="form-group">
                <div class="col-lg-1 col-lg-offset-1">
                    <button class='btn btn-primary' @click='save'>更新</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
import axios from 'axios'
import moment from 'moment'

export default {
    props: {
        buildCase: {
            type: Object,
            required: true
        }
    },
    computed: {
        myDate: {
            get() {
                const date = new moment(this.buildCase.date)
                return date.format('YYYY-MM-DD')
            },
            set(v) {
                this.buildCase.date = v
            }
        },
        lastVisit: {
            get() {
                if (this.buildCase.lastVisit) {
                    const date = new moment(this.buildCase.lastVisit)
                    return date.format('YYYY-MM-DD')
                }else
                    return undefined
            },
            set(v) {
                this.buildCase.lastVisit = v
            }
        },
    },
    methods: {
        save() {
            console.log(this.buildCase)
            axios.put("/BuildCase", this.buildCase).then(
                resp => {
                    let ret = resp.data
                    if (ret.Ok) {
                        alert("成功!")
                    }
                }

            ).catch(err => alert(err))
        }
    },
    components: {
    }
}
</script>
