<template>
    <div class="ibox flot-e-margins">
        <div class="ibox-content">
            <ul class="nav nav-tabs">
                <router-link to='/Order/New' tag='li' role="presentation" active-class='active'><a>新增訂單</a>
                </router-link>
                <router-link to='/Order/Mine' tag='li' role="presentation" active-class='active'><a>我的訂單</a>
                </router-link>
                <router-link to='/Order/Query' tag='li' role="presentation" active-class='active'><a>查詢訂單</a>
                </router-link>
            </ul>
            <router-view></router-view>
        </div>
    </div>
</template>
<style scoped>
</style>
<script>
    export default{
        data(){
            return {
                msg: '訂單主畫面'
            }
        },
        components: {}
    }
</script>
